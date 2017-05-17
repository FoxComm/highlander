package phoenix.utils.apis

import cats.implicits._
import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import failures.Failures
import org.json4s._
import org.json4s.jackson.JsonMethods._
import phoenix.failures.MiddlewarehouseFailures._
import phoenix.payloads.AuthPayload
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import utils.db._

case class SkuInventoryHold(sku: String, qty: Int)

case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit]

  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit]

}

case class MiddlewarehouseErrorInfo(sku: String, debug: String)

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  private def parseListOfStringErrors(strings: Option[List[String]]): Option[Failures] =
    strings.flatMap(errors ⇒ Failures(errors.map(MiddlewarehouseError): _*))

  private def parseListOfMwhInfoErrors(
      maybeErrors: Option[List[MiddlewarehouseErrorInfo]]): Option[Failures] = {
    maybeErrors match {
      case Some(errors) ⇒
        logger.info("Middlewarehouse errors:")
        logger.info(errors.map(_.debug).mkString("\n"))
        logger.info("Check Middlewarehouse logs for more details.")
        Some(SkusOutOfStockFailure(errors.map(_.sku)).single)
      case _ ⇒
        logger.warn("No errors in failed Middlewarehouse response!")
        Some(UnableToHoldLineItems.single)
    }
  }

  //NOTE: This is public only for test purposes
  def parseMwhErrors(message: String): Failures = {
    val json = (parse(message) \ "errors")
    val skuErrors = (json.filterField {
      case JField("sku", _) ⇒ true
      case _                ⇒ false
    })
    val possibleFailures =
      if (skuErrors.isEmpty)
        parseListOfStringErrors(json.extractOpt[List[String]])
      else
        parseListOfMwhInfoErrors(json.extractOpt[List[MiddlewarehouseErrorInfo]])

    possibleFailures.getOrElse(MiddlewarehouseError(message).single)
  }

  override def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold")
    val body   = compact(Extraction.decompose(reservation))
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"middlewarehouse hold: $body")

    val f = Http(req.POST > AsMwhResponse).either.map {
      case Right(MwhResponse(status, _)) if status / 100 == 2 ⇒ Either.right(())
      case Right(MwhResponse(_, message))                     ⇒ Either.left(parseMwhErrors(message))
      case Left(error) ⇒ {
        logger.error(error.getMessage)
        Either.left(UnableToHoldLineItems.single)
      }
    }
    Result.fromFEither(f)
  }

  //Note cart ref becomes order ref num after cart turns into order
  override def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/${orderRefNum}")
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt)
    logger.info(s"middlewarehouse cancel hold: ${orderRefNum}")
    val f = Http(req.DELETE OK as.String).either.map {
      case Right(_)    ⇒ Either.right(())
      case Left(error) ⇒ Either.left(UnableToCancelHoldLineItems.single)
    }
    Result.fromFEither(f)
  }
}

case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
