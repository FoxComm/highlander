package services

import failures.InventoryFailures.InventorySummaryNotFound
import failures.ProductFailures.SkuNotFoundForContext
import models.objects._
import models.inventory._
import models.inventory.summary.InventorySummaries
import models.product.Mvp
import responses.InventoryResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.aliases._

object InventoryManager {

  // Detailed info for SKU of each type in given warehouse
  def getSkuDetails(skuCode: String, warehouseId: Int, context: ObjectContext)
    (implicit ec: EC, db: DB): Result[Seq[SkuDetailsResponse.Root]] = (for {
    sku       ← * <~ Skus.filterByContextAndCode(context.id, skuCode)
      .one.mustFindOr(SkuNotFoundForContext(skuCode, context.name))
    skuForm ← * <~ ObjectForms.mustFindById404(sku.formId)
    skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    warehouse ← * <~ Warehouses.mustFindById404(warehouseId)
    summaries ← * <~ InventorySummaries.findBySkuIdInWarehouse(warehouseId = warehouseId, skuId = sku.id).one
                                       .mustFindOr(InventorySummaryNotFound(sku.id, warehouseId))
  } yield SkuDetailsResponse.build(summaries, Mvp.priceAsInt(skuForm, skuShadow))).run()

  // Summary for sellable SKU across all warehouses
  def getSkuSummary(skuCode: String, context: ObjectContext)
    (implicit ec: EC, db: DB): Result[Seq[SellableSkuSummaryResponse.Root]] = (for {
    sku       ← * <~ Skus.filterByContextAndCode(context.id, skuCode)
      .one.mustFindOr(SkuNotFoundForContext(skuCode, context.name))
    skuForm ← * <~ ObjectForms.mustFindById404(sku.formId)
    skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    summaries ← * <~ InventorySummaries.findSellableBySkuId(sku.id).result.toXor
  } yield summaries.map { 
    case (summary, warehouse) ⇒ 
      SellableSkuSummaryResponse.build(summary, warehouse, Mvp.priceAsInt(skuForm, skuShadow)) 
  }).run()

}
