package phoenix.utils.apis

import cats.implicits._
import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.failures.Failures
import dispatch._
import phoenix.models.inventory.{ProductVariantSku, ProductVariantSkus}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.{compactJson, parseJsonOpt}
import phoenix.failures.MiddlewarehouseFailures._
import phoenix.payloads.AuthPayload
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

case class SkuInventoryHold(sku: String, qty: Int)

case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])

case class CreateSku(code: String,
                     taxClass: String = "default",
                     requiresShipping: Boolean = true,
                     shippingClass: String = "default",
                     requiresInventoryTracking: Boolean = true)

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit]

  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit]

  def createSku(skuId: Int, sku: CreateSku)(implicit ec: EC, au: AU): DbResultT[ProductVariantSku]
}

case class MwhErrorInfo(sku: String, afs: Int, debug: String)

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  private def parseListOfStringErrors(strings: Option[List[String]]): Option[Failures] =
    strings.flatMap(errors ⇒ Failures(errors.map(MiddlewarehouseError): _*))

  private def parseListOfMwhInfoErrors(maybeErrors: Option[List[MwhErrorInfo]]): Option[Failures] =
    maybeErrors match {
      case Some(errors) ⇒
        logger.info("Middlewarehouse errors:")
        logger.info(errors.map(_.debug).mkString("\n"))
        logger.info("Check Middlewarehouse logs for more details.")
        Some(SkusOutOfStockFailure(errors).single)
      case _ ⇒
        logger.warn("No errors in failed Middlewarehouse response!")
        Some(UnexpectedMwhResponseFailure.single)
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
        parseListOfMwhInfoErrors(json.extractOpt[List[MwhErrorInfo]])

    possibleFailures.getOrElse(MiddlewarehouseError(message).single)
  }

  override def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold")
    val body   = compact(Extraction.decompose(reservation))
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"Middlewarehouse hold: $body")

    val f = Http(req.POST > AsMwhResponse).either.map {
      case Right(MwhResponse(status, _)) if status / 100 == 2 ⇒ Either.right(())
      case Right(MwhResponse(_, message))                     ⇒ Either.left(parseMwhErrors(message))
      case Left(error) ⇒
        logger.error(s"Hold items failed with message '${error.getMessage}'")
        Either.left(MwhConnectionFailure.single)
    }
    Result.fromFEither(f)
  }

  //Note cart ref becomes order ref num after cart turns into order
  override def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/$orderRefNum")
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt)
    logger.info(s"Middlewarehouse cancel hold: $orderRefNum")
    val f = Http(req.DELETE OK as.String).either.map {
      case Right(_) ⇒ Either.right(())
      case Left(error) ⇒
        logger.error(s"Cancel hold items failed with message '${error.getMessage}")
        Either.left(MwhConnectionFailure.single)
    }
    Result.fromFEither(f)
  }

  // Returns newly created SKU id
  override def createSku(skuId: Int, sku: CreateSku)(implicit ec: EC, au: AU): DbResultT[ProductVariantSku] = {
    val reqUrl = dispatch.url(s"$url/v1/public/skus")
    val body   = compact(Extraction.decompose(sku))
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"Middlewarehouse create sku: $body")

    DbResultT.fromResult(Result.fromF(Http(req.POST > AsMwhResponse).either)).flatMap {
      case Right(MwhResponse(status, body)) if status / 100 == 2 ⇒
        extractAndSaveSkuId(skuId, body)
      case Right(MwhResponse(status, message)) ⇒
        logger.error(s"SKU creation request failed with status code '$status' and message '$message'")
        DbResultT.failures(parseMwhErrors(message))
      case Left(error) ⇒
        logger.error(s"SKU creation request failed with message '${error.getMessage}'")
        DbResultT.failure(MwhConnectionFailure)
    }
  }

  private def extractAndSaveSkuId(skuId: Int, responseBody: String)(
      implicit ec: EC): DbResultT[ProductVariantSku] =
    parseJsonOpt(responseBody) match {
      case Some(json) ⇒
        (json \ "id").extractOpt[Int] match {
          case Some(mwhSkuId) ⇒
            logger.debug(s"Successfully created new SKU in MWH, ID=$mwhSkuId")
            ProductVariantSkus.create(ProductVariantSku(skuId = skuId, mwhSkuId = mwhSkuId))
          case _ ⇒
            logger.error(
              s"Unable to find ID in MWH SKU creation response. JSON body was:\n${compactJson(json)}")
            DbResultT.failure[ProductVariantSku](UnexpectedMwhResponseFailure)
        }
      case _ ⇒
        logger.error(s"Unable to parse MWH response as JSON. Response body was:\n$responseBody")
        DbResultT.failure[ProductVariantSku](UnexpectedMwhResponseFailure)
    }
}

case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
