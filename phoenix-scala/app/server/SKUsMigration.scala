package server

import java.util.concurrent.TimeUnit
import models.inventory.{ProductVariantMwhSkuIds, ProductVariants}
import scala.concurrent.Await
import utils.apis.{CreateSku, MiddlewarehouseApi}
import utils.db._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try
import slick.driver.PostgresDriver.api._
import utils.FoxConfig
import utils.aliases._

class SKUsMigration(api: MiddlewarehouseApi) {
  lazy val batchSize: Int = Try(FoxConfig.config.getInt("migrations.batchSize")).getOrElse(100)
  lazy val timeout: Duration =
    Try(FoxConfig.config.getDuration("migrations.timeout", TimeUnit.SECONDS).seconds)
      .getOrElse(Duration.Inf)

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
