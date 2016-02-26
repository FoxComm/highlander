package services.inventory

import java.time.Instant
import models.inventory.Warehouse.HARDCODED_WAREHOUSE_ID
import models.inventory.adjustment.InventoryAdjustment._
import models.inventory.adjustment._
import models.inventory.summary.{InventorySummaries, SellableInventorySummaries}
import models.order.Order
import models.order.lineitems.OrderLineItemSkus
import org.json4s.Extraction.{decompose ⇒ toJson}
import services.InventorySummaryNotFound
import utils.Slick.DbResult
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.Slick.implicits._
import utils.aliases._

object InventoryAdjustmentManager {
  implicit val formats = JsonFormatters.phoenixFormats

  def orderPlaced(order: Order)(implicit ec: EC, db: DB): DbResult[Seq[SellableInventoryAdjustment]] = {
    OrderLineItemSkus.findByOrderId(order.id).map(_.skuId).result.flatMap { skuIds ⇒
      DbResultT.sequence(skuIds.map(skuId ⇒ for {
        sellable   ← * <~ findSellableSummary(skuId = skuId, warehouseId = HARDCODED_WAREHOUSE_ID)
        event      = OrderPlaced(warehouseId = 1, skuId = skuId, orderRef = order.refNum, quantity = 1)
        adjustment ← * <~ SellableInventoryAdjustments.create(SellableInventoryAdjustment(summaryId = sellable.id,
                            onHoldChange = event.quantity, metadata = toJson(event)))
        _          ← * <~ SellableInventorySummaries.update(sellable, sellable.copy(onHold = sellable.onHold + 1,
                                                                                    updatedAt = Instant.now))
      } yield adjustment)).value
    }
  }

  // TODO: consider refactoring to `order: Order` param as well
  def orderPropagated(event: OrderPropagated)(implicit ec: EC, db: DB) = for {
    sellable   ← * <~ findSellableSummary(event.skuId, event.warehouseId)
    // Move qty from onHold to reserved
    adjustment ← * <~ SellableInventoryAdjustments.create(SellableInventoryAdjustment(summaryId = sellable.id,
                        onHoldChange = -event.quantity, reservedChange = event.quantity, metadata = toJson(event)))
    _          ← * <~ SellableInventorySummaries.update(sellable, sellable.copy(
                        onHold = sellable.onHold - event.quantity,
                        reserved = sellable.reserved + event.quantity,
                        updatedAt = Instant.now))
  } yield adjustment

  def wmsOverride(event: WmsOverride)(implicit ec: EC, db: DB) = for {
    sellable   ← * <~ findSellableSummary(event.skuId, event.warehouseId)
    adjustment ← * <~ SellableInventoryAdjustments.create(SellableInventoryAdjustment(summaryId = sellable.id,
                        onHandChange = event.onHand - sellable.onHand,
                        onHoldChange = event.onHold - sellable.onHold,
                        reservedChange = event.reserved - sellable.reserved,
                        metadata = toJson(event)))
    _          ← * <~ SellableInventorySummaries.update(sellable, sellable.copy(onHand = event.onHand,
                        onHold = event.onHold, reserved = event.reserved, updatedAt = Instant.now))
  } yield adjustment

  private def findSellableSummary(skuId: Int, warehouseId: Int)(implicit ec: EC) =
    InventorySummaries.findSellableBySkuIdInWarehouse(skuId = skuId, warehouseId = warehouseId).one
                      .mustFindOr(InventorySummaryNotFound(skuId, warehouseId))

}
