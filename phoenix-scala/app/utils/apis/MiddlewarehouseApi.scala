package utils.apis

import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import failures.MiddlewarehouseFailures._
import failures.{Failures, MiddlewarehouseFailures}
import models.inventory.{ProductVariantMwhSkuId, ProductVariantMwhSkuIds}
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.{compactJson, parseJsonOpt}
import services.Result
import utils.JsonFormatters
import payloads.AuthPayload
import utils.aliases._
import utils.db._

final case class SkuInventoryHold(sku: String, qty: Int)
final case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])
final case class CreateSku(code: String, taxClass: String = "default")
final case class CreateSkuBatchElement(variantFormId: Int, cmd: CreateSku)

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit]
  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit]
  def createSku(variantFormId: Int, sku: CreateSku)(implicit ec: EC,
                                                    au: AU): DbResultT[ProductVariantMwhSkuId]
  def createSkus(skusToCreate: Seq[CreateSkuBatchElement], batchSize: Int)(
      implicit ec: EC,
      au: AU): DbResultT[Vector[ProductVariantMwhSkuId]]
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

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/$orderRefNum")
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt)
    logger.info(s"middlewarehouse cancel hold: $orderRefNum")
    Http(req.DELETE OK as.String).either.flatMap {
      case Right(_)    ⇒ Result.unit
      case Left(error) ⇒ Result.failure(MiddlewarehouseFailures.UnableToCancelHoldLineItems)
    }
  }

  // Returns newly created SKU id
  override def createSku(variantFormId: Int, sku: CreateSku)(
      implicit ec: EC,
      au: AU): DbResultT[ProductVariantMwhSkuId] = {
    val reqUrl = dispatch.url(s"$url/v1/public/skus")
    val body   = compact(Extraction.decompose(sku))
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"middlewarehouse create sku: $body")

    DbResultT.fromFuture(Http(req.POST > AsMwhResponse).either).flatMap {
      case Right(MwhResponse(status, body)) if status / 100 == 2 ⇒
        extractAndSaveSkuId(variantFormId, body)

      case Right(MwhResponse(status, message)) ⇒
        logger.error(
            s"SKU creation request failed with status code '$status' and message '$message'")
        DbResultT.failures(parseMwhErrors(message))

      case Left(error) ⇒
        logger.error(s"SKU creation request failed with message '$error'")
        DbResultT.failure(UnableToCreateSku)
    }
  }

  private def extractAndSaveSkuId(variantFormId: Int, responseBody: String)(
      implicit ec: EC): DbResultT[ProductVariantMwhSkuId] =
    parseJsonOpt(responseBody) match {
      case Some(json) ⇒
        (json \ "id").extractOpt[Int] match {
          case Some(skuId) ⇒
            logger.debug(s"Successfully created new SKU in MWH, ID=$skuId")
            ProductVariantMwhSkuIds.create(
                ProductVariantMwhSkuId(variantFormId = variantFormId, mwhSkuId = skuId))
          case _ ⇒
            logger.error(
                s"Unable to find ID in MWH SKU creation response. JSON body was:\n${compactJson(json)}")
            DbResultT.failure[ProductVariantMwhSkuId](NoSkuIdInResponse)
        }
      case _ ⇒
        logger.error(s"Unable to parse MWH response as JSON. Response body was:\n$responseBody")
        DbResultT.failure[ProductVariantMwhSkuId](UnableToParseResponse)
    }

  // TODO send real batched request to MWH
  protected def executeSkusBatch(batch: Seq[CreateSkuBatchElement])(
      implicit ec: EC,
      au: AU): DbResultT[Vector[ProductVariantMwhSkuId]] = {
    DbResultT.sequence(batch.map {
      case CreateSkuBatchElement(formId, cmd) ⇒ createSku(formId, cmd)
    }(collection.breakOut))
  }

  def createSkus(skusToCreate: Seq[CreateSkuBatchElement], batchSize: Int = 100)(
      implicit ec: EC,
      au: AU): DbResultT[Vector[ProductVariantMwhSkuId]] = {
    if (skusToCreate.nonEmpty)
      skusToCreate
        .grouped(if (batchSize > 0) batchSize else skusToCreate.length)
        .foldLeft(DbResultT.good(Vector.empty[ProductVariantMwhSkuId])) { (acc, batch) ⇒
          for {
            ids    ← acc
            newIds ← executeSkusBatch(batch)
          } yield ids ++ newIds
        } else DbResultT.good(Vector.empty)
  }
}

final case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
