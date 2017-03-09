package utils.apis

import cats.data.{StateT, Xor, XorT}
import cats.implicits._
import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import failures.MiddlewarehouseFailures._
import failures.{Failures, GeneralFailure, MiddlewarehouseFailures}
import models.inventory.{ProductVariantSku, ProductVariantSkus}
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.{compactJson, parseJsonOpt}
import utils.JsonFormatters
import payloads.AuthPayload
import utils.JsonFormatters
import utils.aliases._
import utils.db._

case class SkuInventoryHold(sku: String, qty: Int)
case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])
case class CreateSku(code: String, taxClass: String = "default")

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit]
  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit]
  def createSku(variantFormId: Int, sku: CreateSku)(implicit ec: EC,
                                                    au: AU): DbResultT[ProductVariantSku]
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

    val f = Http(req.POST > AsMwhResponse).either.map {
      case Right(MwhResponse(status, _)) if status / 100 == 2 ⇒ Xor.right(())
      case Right(MwhResponse(_, message))                     ⇒ Xor.left(parseMwhErrors(message))
      case Left(error)                                        ⇒ Xor.left(MiddlewarehouseFailures.UnableToHoldLineItems.single)
    }
    Result.fromFXor(f)
  }

  //Note cart ref becomes order ref num after cart turns into order
  override def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/$orderRefNum")
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt)
    logger.info(s"middlewarehouse cancel hold: $orderRefNum")
    val f = Http(req.DELETE OK as.String).either.map {
      case Right(_)    ⇒ Xor.right(())
      case Left(error) ⇒ Xor.left(MiddlewarehouseFailures.UnableToCancelHoldLineItems.single)
    }
    Result.fromFXor(f)
  }

  // Returns newly created SKU id
  override def createSku(variantFormId: Int,
                         sku: CreateSku)(implicit ec: EC, au: AU): DbResultT[ProductVariantSku] = {
    val reqUrl = dispatch.url(s"$url/v1/public/skus")
    val body   = compact(Extraction.decompose(sku))
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"middlewarehouse create sku: $body")

    DbResultT.fromResult(Result.fromF(Http(req.POST > AsMwhResponse).either)).flatMap {
      case Right(MwhResponse(status, body)) if status / 100 == 2 ⇒
        extractAndSaveSkuId(variantFormId, sku.code, body)

      case Right(MwhResponse(status, message)) ⇒
        logger.error(
            s"SKU creation request failed with status code '$status' and message '$message'")
        DbResultT.failures(parseMwhErrors(message))

      case Left(error) ⇒
        logger.error(s"SKU creation request failed with message '$error'")
        DbResultT.failure(UnableToCreateSku)
    }
  }

  private def extractAndSaveSkuId(variantFormId: Int, skuCode: String, responseBody: String)(
      implicit ec: EC): DbResultT[ProductVariantSku] =
    parseJsonOpt(responseBody) match {
      case Some(json) ⇒
        (json \ "id").extractOpt[Int] match {
          case Some(skuId) ⇒
            logger.debug(s"Successfully created new SKU in MWH, ID=$skuId")
            ProductVariantSkus.create(
                ProductVariantSku(variantFormId = variantFormId, skuId = skuId, skuCode = skuCode))
          case _ ⇒
            logger.error(
                s"Unable to find ID in MWH SKU creation response. JSON body was:\n${compactJson(json)}")
            DbResultT.failure[ProductVariantSku](NoSkuIdInResponse)
        }
      case _ ⇒
        logger.error(s"Unable to parse MWH response as JSON. Response body was:\n$responseBody")
        DbResultT.failure[ProductVariantSku](UnableToParseResponse)
    }
}

case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
