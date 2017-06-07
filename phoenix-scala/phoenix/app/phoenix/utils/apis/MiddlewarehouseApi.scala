package phoenix.utils.apis

import cats.implicits._
import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.failures.Failures
import dispatch._
import models.inventory.{Sku2MwhSku, Sku2MwhSkus}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.{compactJson, parseJsonOpt}
import phoenix.failures.MiddlewarehouseFailures._
import phoenix.payloads.AuthPayload
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

case class SkuInventoryHold(sku: String, qty: Int)

case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])

case class CreateSku(code: String, taxClass: String = "default")

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit]

  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit]

  def createSku(skuId: Int, sku: CreateSku)(implicit ec: EC, au: AU): DbResultT[Sku2MwhSku]
}

case class MiddlewarehouseErrorInfo(sku: String, debug: String)

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  private def parseListOfStringErrors(strings: Option[List[String]]): Option[Failures] =
    strings.flatMap(errors ⇒ Failures(errors.map(MiddlewarehouseError): _*))

  private def parseListOfMwhInfoErrors(
      maybeErrors: Option[List[MiddlewarehouseErrorInfo]]): Option[Failures] =
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

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/$orderRefNum")
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt)
    logger.info(s"middlewarehouse cancel hold: $orderRefNum")
    val f = Http(req.DELETE OK as.String).either.map {
      case Right(_)    ⇒ Either.right(())
      case Left(error) ⇒ Either.left(UnableToCancelHoldLineItems.single)
    }
    Result.fromFEither(f)
  }

  // Returns newly created SKU id
  override def createSku(skuId: Int, sku: CreateSku)(implicit ec: EC, au: AU): DbResultT[Sku2MwhSku] = {
    val reqUrl = dispatch.url(s"$url/v1/public/skus")
    val body   = compact(Extraction.decompose(sku))
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"middlewarehouse create sku: $body")

    DbResultT.fromResult(Result.fromF(Http(req.POST > AsMwhResponse).either)).flatMap {
      case Right(MwhResponse(status, body)) if status / 100 == 2 ⇒
        extractAndSaveSkuId(skuId, body)

      case Right(MwhResponse(status, message)) ⇒
        logger.error(s"SKU creation request failed with status code '$status' and message '$message'")
        DbResultT.failures(parseMwhErrors(message))

      case Left(error) ⇒
        logger.error(s"SKU creation request failed with message '$error'")
        DbResultT.failure(UnableToCreateSku)
    }
  }

  private def extractAndSaveSkuId(skuId: Int, responseBody: String)(implicit ec: EC): DbResultT[Sku2MwhSku] =
    parseJsonOpt(responseBody) match {
      case Some(json) ⇒
        (json \ "id").extractOpt[Int] match {
          case Some(mwhSkuId) ⇒
            logger.debug(s"Successfully created new SKU in MWH, ID=$mwhSkuId")
            Sku2MwhSkus.create(Sku2MwhSku(skuId = skuId, mwhSkuId = mwhSkuId))
          case _ ⇒
            logger.error(
              s"Unable to find ID in MWH SKU creation response. JSON body was:\n${compactJson(json)}")
            DbResultT.failure[Sku2MwhSku](NoSkuIdInResponse)
        }
      case _ ⇒
        logger.error(s"Unable to parse MWH response as JSON. Response body was:\n$responseBody")
        DbResultT.failure[Sku2MwhSku](UnableToParseResponse)
    }
}

case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
