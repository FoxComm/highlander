package server

import models.inventory.{ProductVariantMwhSkuIds, ProductVariants}
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.{Apis, CreateSku, CreateSkuBatchElement, MiddlewarehouseApi}
import utils.db._

object SkusMigration {
  @inline private def getUnsyncedSKUs =
    for {
      (pv, mwh) ← ProductVariants
                   .joinLeft(ProductVariantMwhSkuIds)
                   .on(_.formId === _.variantFormId) if mwh.isEmpty
    } yield (pv.formId, pv.code)

  def migrate(batchSize: Int)(implicit apis: Apis, ec: EC, db: DB, au: AU): DbResultT[Unit] = {
    for {
      pv ← * <~ getUnsyncedSKUs.result
      _ ← * <~ apis.middlwarehouse.createSkus(pv.map {
           case (formId, code) ⇒ CreateSkuBatchElement(formId, CreateSku(code))
         }, batchSize)
    } yield ()
  }
}
