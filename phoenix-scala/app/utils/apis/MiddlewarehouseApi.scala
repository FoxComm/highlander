package utils.apis

import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import failures.MiddlewarehouseFailures.MiddlewarehouseError
import failures.{Failures, MiddlewarehouseFailures}
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods._
import services.Result
import utils.JsonFormatters
import payloads.AuthPayload
import utils.aliases._

case class SkuInventoryHold(skuId: Integer, qty: Int)
case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit]
  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit]

}

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  def parseMwhErrors(message: String): Failures = {
    val errorString = (parse(message) \ "errors").extractOpt[List[String]]
    errorString
      .flatMap(errors ⇒ Failures(errors.map(MiddlewarehouseError): _*))
      .getOrElse(MiddlewarehouseError(message).single)
  }

  override def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold")
    val body   = compact(Extraction.decompose(reservation))
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"middlewarehouse hold: $body")

    Http(req.POST > AsMwhResponse).either.flatMap {
      case Right(MwhResponse(status, _)) if status / 100 == 2 ⇒ Result.unit
      case Right(MwhResponse(_, message))                     ⇒ Result.failures(parseMwhErrors(message))
      case Left(error)                                        ⇒ Result.failure(MiddlewarehouseFailures.UnableToHoldLineItems)
    }
  }

  //Note cart ref becomes order ref num after cart turns into order
  override def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/${orderRefNum}")
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt)
    logger.info(s"middlewarehouse cancel hold: ${orderRefNum}")
    Http(req.DELETE OK as.String).either.flatMap {
      case Right(_)    ⇒ Result.unit
      case Left(error) ⇒ Result.failure(MiddlewarehouseFailures.UnableToCancelHoldLineItems)
    }
  }
}

case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
