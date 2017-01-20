package server

import models.inventory.{ProductVariantMwhSkuIds, ProductVariants}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.PostgresDriver.api._
import utils.FoxConfig._
import utils.aliases._
import utils.apis.{Apis, CreateSku, CreateSkuBatchElement, MiddlewarehouseApi}
import utils.db._

object SkusMigration {

  lazy val batchSize: Int    = config.getOptInt("migrations.batchSize").getOrElse(100)
  lazy val timeout: Duration = config.getOptDuration("migrations.timeout").getOrElse(Duration.Inf)

  @inline private def getUnsyncedSKUs =
    for {
      (pv, mwh) ← ProductVariants
                   .joinLeft(ProductVariantMwhSkuIds)
                   .on(_.formId === _.variantFormId) if mwh.isEmpty
    } yield (pv.formId, pv.code)

  def run()(implicit apis: Apis, ec: EC, db: DB, au: AU) = {
    val migration: DbResultT[Unit] = {
      for {
        pv ← * <~ getUnsyncedSKUs.result
        _ ← * <~ apis.middlwarehouse.createSkus(pv.map {
             case (formId, code) ⇒ CreateSkuBatchElement(formId, CreateSku(code))
           }, batchSize)
      } yield ()
    }

    Await.result(migration.runTxn(), timeout)
  }
}
