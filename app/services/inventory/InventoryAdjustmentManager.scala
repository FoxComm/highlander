package services.inventory

import java.time.Instant

import models.inventory.{InventoryAdjustment ⇒ Adj, InventoryAdjustments ⇒ Adjs, _}
import models.inventory.Warehouse.HARDCODED_WAREHOUSE_ID
import Adj._
import failures.InventoryFailures.InventorySummaryNotFound
import models.inventory.summary._
import models.order.Order
import models.order.lineitems.OrderLineItemSkus
import org.json4s.Extraction.{decompose ⇒ toJson}
import utils.JsonFormatters
import utils.aliases._
import utils.db._
import utils.db.DbResultT._
import utils.db.ExPostgresDriver.api._

object InventoryAdjustmentManager {
  implicit val formats = JsonFormatters.phoenixFormats

  def orderPlaced(order: Order)(implicit ec: EC): DbResult[Iterable[Adj]] =
    lineItemSkus(order.id).flatMap { skuIds ⇒
      DbResultT.sequence(groupSkusByQtys(skuIds).map { case (skuId, qty) ⇒
        for {
          sum ← * <~ findSellableSummary(skuId = skuId, warehouseId = HARDCODED_WAREHOUSE_ID)
          event = OrderPlaced(warehouseId = HARDCODED_WAREHOUSE_ID, skuId = skuId, orderRef = order.refNum, quantity = qty)
          adj ← * <~ Adjs.create(Adj(summaryId = sum.id, change = event.quantity, state = OnHold, skuType = Sellable,
                       metadata = toJson(event), newQuantity = sum.onHold + qty, newAfs = sum.availableForSale - qty))
          _   ← * <~ SellableInventorySummaries.update(sum, sum.copy(onHold = sum.onHold + qty, updatedAt = Instant.now))
        } yield adj
      }).value
    }

  def orderPropagated(order: Order)(implicit ec: EC) =
    lineItemSkus(order.id).flatMap { skuIds ⇒
      DbResultT.sequence(groupSkusByQtys(skuIds).map { case (skuId, qty) ⇒
        for {
          sum ← * <~ findSellableSummary(skuId = skuId, warehouseId = HARDCODED_WAREHOUSE_ID)
          event = OrderPropagated(warehouseId = HARDCODED_WAREHOUSE_ID, skuId = skuId, orderRef = order.refNum, quantity = qty)
          metadata = toJson(event)
          adj ← * <~ Adjs.createAll(Seq(
                       Adj(summaryId = sum.id, skuType = Sellable, state = OnHold, change = -qty,
                         newQuantity = sum.onHold - qty, newAfs = sum.availableForSale + qty, metadata = metadata),
                       Adj(summaryId = sum.id, skuType = Sellable, state = Reserved, change = qty,
                         newQuantity = sum.reserved + qty, newAfs = sum.availableForSale, metadata = metadata)
                     ))
          _   ← * <~ SellableInventorySummaries.update(sum, sum.copy(
                       onHold = sum.onHold - qty, reserved = sum.reserved + qty, updatedAt = Instant.now))
        } yield adj
      }).value
    }

  def wmsOverride(event: WmsOverride)(implicit ec: EC): DbResultT[Seq[Int]] = for {
    sum ← * <~ findSellableSummary(event.skuId, event.warehouseId)
    adj ← * <~ Adjs.createAllReturningIds(generateAdjustmentsForEvent(sum, event))
    _   ← * <~ SellableInventorySummaries.update(sum, sum.copy(onHand = event.onHand, onHold = event.onHold,
                 reserved = event.reserved, updatedAt = Instant.now))
  } yield adj

  private def findSellableSummary(skuId: Int, warehouseId: Int)(implicit ec: EC) =
    InventorySummaries.findSellableBySkuIdInWarehouse(skuId = skuId, warehouseId = warehouseId).one
                      .mustFindOr(InventorySummaryNotFound(skuId, warehouseId))

  private def groupSkusByQtys(skuIds: Seq[Int]): Map[Int, Int] =
    skuIds.foldLeft(Map[Int, Int]()) { (map, skuId) ⇒
      val quantity = map.getOrElse(skuId, 0)
      map.updated(skuId, quantity + 1)
    }

  private def lineItemSkus(orderId: Int) = OrderLineItemSkus.findByOrderId(orderId).map(_.skuId).result

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

    // Split WMS update into multiple adjustments for each state
    val allStates = Seq(
      (summary.onHand, event.onHand, OnHand),
      (summary.onHold, event.onHold, OnHold),
      (summary.reserved, event.reserved, Reserved)
    )

    // Recalculate new AFS after each adjustment
    allStates
      // Make sure we don't create adjustment with change = 0
      .filterNot { case (currentQty, newQty, _) ⇒ currentQty == newQty }
      // Use previously calculated AFS. If this is the first adjustment for this event, use AFS from inventory summary
      .foldLeft(List[Adj]()) { case (accum, (currentQty, newQty, state)) ⇒
        val afs = accum.headOption.map(_.newAfs).getOrElse(summary.availableForSale)
        buildAdjustment(currentQty, newQty, state, afs) :: accum
      }
  }
}
