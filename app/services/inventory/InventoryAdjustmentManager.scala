package services.inventory

import java.time.Instant

import models.inventory.{InventoryAdjustment ⇒ Adj, InventoryAdjustments ⇒ Adjs, _}
import models.inventory.Warehouse.HARDCODED_WAREHOUSE_ID
import Adj._
import models.inventory.summary._
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

  def orderPlaced(order: Order)(implicit ec: EC, db: DB): DbResult[Iterable[Adj]] = {
    OrderLineItemSkus.findByOrderId(order.id).map(_.skuId).result.flatMap { skuIds ⇒
      // Group SKUs to get quantity for each SKU
      val skusQtys = skuIds.foldLeft(Map[Int, Int]()) { (map, skuId) ⇒
        val quantity = map.getOrElse(skuId, 0)
        map.updated(skuId, quantity + 1)
      }

      DbResultT.sequence(skusQtys.map { case (skuId, qty) ⇒
        for {
          sum ← * <~ findSellableSummary(skuId = skuId, warehouseId = HARDCODED_WAREHOUSE_ID)
          event = OrderPlaced(warehouseId = 1, skuId = skuId, orderRef = order.refNum, quantity = qty)
          adj ← * <~ Adjs.create(Adj(summaryId = sum.id, change = event.quantity, state = OnHold, skuType = Sellable,
                       metadata = toJson(event), newQuantity = sum.onHold + qty, newAfs = sum.availableForSale - qty))
          _   ← * <~ SellableInventorySummaries.update(sum, sum.copy(onHold = sum.onHold + qty, updatedAt = Instant.now))
        } yield adj
      }).value
    }
  }

  // TODO: consider refactoring to `order: Order` param as well
  def orderPropagated(event: OrderPropagated)(implicit ec: EC, db: DB) = for {
    sum ← * <~ findSellableSummary(event.skuId, event.warehouseId)
    // Move qty from onHold to reserved
    metadata = toJson(event)
    qty = event.quantity
    adj ← * <~ Adjs.createAll(Seq(
                 Adj(summaryId = sum.id, skuType = Sellable, state = OnHold, change = -qty,
                   newQuantity = sum.onHold - qty, newAfs = sum.availableForSale + qty, metadata = metadata),
                 Adj(summaryId = sum.id, skuType = Sellable, state = Reserved, change = qty,
                   newQuantity = sum.reserved + qty, newAfs = sum.availableForSale, metadata = metadata)
               ))
    _ ← * <~ SellableInventorySummaries.update(sum, sum.copy(
               onHold = sum.onHold - qty, reserved = sum.reserved + qty, updatedAt = Instant.now))
  } yield adj

  def wmsOverride(event: WmsOverride)(implicit ec: EC, db: DB): DbResultT[Seq[Int]] = for {
    sum ← * <~ findSellableSummary(event.skuId, event.warehouseId)
    adj ← * <~ Adjs.createAllReturningIds(generateAdjustmentsForEvent(sum, event))
    _   ← * <~ SellableInventorySummaries.update(sum, sum.copy(onHand = event.onHand, onHold = event.onHold,
                 reserved = event.reserved, updatedAt = Instant.now))
  } yield adj

  private def findSellableSummary(skuId: Int, warehouseId: Int)(implicit ec: EC) =
    InventorySummaries.findSellableBySkuIdInWarehouse(skuId = skuId, warehouseId = warehouseId).one
                      .mustFindOr(InventorySummaryNotFound(skuId, warehouseId))

  private def generateAdjustmentsForEvent(summary: SellableInventorySummary, event: WmsOverride): Seq[Adj] = {
    lazy val metadata = toJson(event)

    def buildAdjustment(currentQty: Int, newQty: Int, state: State, currentAfs: Int): Adj = {
      val change = newQty - currentQty
      val newAfs = state match {
        case OnHand ⇒ currentAfs + change
        case      _ ⇒ currentAfs - change
      }
      Adj(change = change, newQuantity = newQty, state = state, metadata = metadata, skuType = Sellable,
        newAfs = newAfs, summaryId = summary.id)
    }

    Seq(
      (summary.onHand, event.onHand, OnHand),
      (summary.onHold, event.onHold, OnHold),
      (summary.reserved, event.reserved, Reserved)
    ).filterNot { case (currentQty, newQty, _) ⇒ currentQty == newQty }
     .foldLeft(List[Adj]()) { case (accum, (currentQty, newQty, state)) ⇒
       val afs = accum.headOption.map(_.newAfs).getOrElse(summary.availableForSale)
       buildAdjustment(currentQty, newQty, state, afs) :: accum
     }
  }
}
