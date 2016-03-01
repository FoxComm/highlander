package services

import models.inventory._
import models.inventory.summary.InventorySummaries
import responses.InventoryResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.aliases._

object InventoryManager {

  // Detailed info for SKU of each type in given warehouse
  def getSkuDetails(skuCode: String, warehouseId: Int)
    (implicit ec: EC, db: DB): Result[Seq[SkuDetailsResponse.Root]] = (for {
    sku       ← * <~ Skus.mustFindByCode(skuCode)
    _         ← * <~ Warehouses.mustFindById404(warehouseId)
    summaries ← * <~ InventorySummaries.findBySkuIdInWarehouse(warehouseId = warehouseId, skuId = sku.id).one
                                       .mustFindOr(InventorySummaryNotFound(sku.id, warehouseId))
  } yield SkuDetailsResponse.build(summaries, sku.price)).run()

  // Summary for sellable SKU across all warehouses
  def getSkuSummary(skuCode: String)(implicit ec: EC, db: DB): Result[Seq[SellableSkuSummaryResponse.Root]] = (for {
    sku       ← * <~ Skus.mustFindByCode(skuCode)
    summaries ← * <~ InventorySummaries.findSellableBySkuId(sku.id).result.toXor
  } yield summaries.map { case (summary, warehouse) ⇒ SellableSkuSummaryResponse.build(summary, warehouse, sku.price) }).run()

}
