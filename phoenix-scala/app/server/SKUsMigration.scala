package server

import models.inventory.{ProductVariantMwhSkuIds, ProductVariants}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.PostgresDriver.api._
import utils.FoxConfig._
import utils.aliases._
import utils.apis.{CreateSku, MiddlewarehouseApi}
import utils.db._

class SKUsMigration(api: MiddlewarehouseApi) {

  lazy val batchSize: Int    = config.getOptInt("migrations.batchSize").getOrElse(100)
  lazy val timeout: Duration = config.getOptDuration("migrations.timeout").getOrElse(Duration.Inf)

  @inline private def getMissingSKUs =
    for {
      (pv, mwh) ← ProductVariants
                   .joinLeft(ProductVariantMwhSkuIds)
                   .on(_.formId === _.variantFormId) if mwh.isEmpty
    } yield (pv.formId, pv.code)

  def run()(implicit ec: EC, db: DB, au: AU) = {
    val migration: DbResultT[Unit] = {
      for {
        pv ← * <~ getMissingSKUs.result
        _ ← * <~ api.createSkus(pv.map { case (formId, code) ⇒ formId → CreateSku(code) },
                                batchSize)
      } yield ()
    }

    Await.result(migration.runTxn(), timeout)
  }
}
