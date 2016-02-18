package services

import scala.concurrent.ExecutionContext

import models.inventory._
import responses.InventoryResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

object InventoryManager {

  // Detailed info for SKU of each type in given warehouse
  def getSkuDetails(skuCode: String, warehouseId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Seq[SkuDetailsResponse.Root]] = (for {
    sku       ← * <~ Skus.mustFindByCode(skuCode)
    warehouse ← * <~ Warehouses.mustFindById404(warehouseId)
    summaries ← * <~ InventorySummaries.findBySkuIdInWarehouse(warehouseId = warehouseId, id = sku.id).result.toXor
  } yield SkuDetailsResponse.build(summaries, warehouse, sku.price)).run()

  // Summary for sellable SKU across all warehouses
  def getSkuSummary(skuCode: String)
    (implicit ec: ExecutionContext, db: Database): Result[Seq[SkuSummaryResponse.Root]] = (for {
    sku       ← * <~ Skus.mustFindByCode(skuCode)
    summaries ← * <~ InventorySummaries.findSellableBySkuId(sku.id).result.toXor
  } yield summaries.map { case (summary, warehouse) ⇒ SkuSummaryResponse.build(summary, warehouse, sku.price) }).run()

}
