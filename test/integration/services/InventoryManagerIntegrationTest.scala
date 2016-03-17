package services

import scala.concurrent.ExecutionContext.Implicits.global

import models.StoreAdmins
import models.activity.ActivityContext
import models.inventory.InventoryAdjustment._
import models.inventory._
import models.inventory.summary._
import models.order._
import models.product.{ProductContexts, SimpleContext}
import payloads.UpdateLineItemsPayload
import services.inventory.InventoryAdjustmentManager
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.ProductSeeds
import utils.seeds.Seeds.Factories
import utils.seeds.generators._

class InventoryManagerIntegrationTest extends IntegrationTestBase {

  implicit val activityContext = ActivityContext(userId = 1, userType = "admin", transactionId = "foo")

  "Inventory adjustment manager" - {
    "adjusts inventory on order placement" in new Fixture {
      InventoryAdjustmentManager.orderPlaced(order).run().futureValue.rightVal

      val summary = SellableInventorySummaries.findOneById(sellable.id).run().futureValue.value
      summary.onHand must === (sellable.onHand)
      summary.onHold must === (sellable.onHold + 2)
      summary.reserved must === (sellable.reserved)
      summary.safetyStock must === (sellable.safetyStock)

      val adjustments = InventoryAdjustments.findSellableBySummaryId(sellable.id).result.run().futureValue
      adjustments must have size 1
      adjustments.filterNot(_.state == OnHold) mustBe empty
      val onHoldAdj = adjustments.headOption.value
      onHoldAdj.state must === (OnHold)
      onHoldAdj.change must === (2)
      onHoldAdj.newAfs must === (sellable.availableForSale - 2)
      onHoldAdj.newQuantity must === (sellable.onHold + 2)
    }

    "adjusts inventory on order propagation to WMS" in new Fixture {
      InventoryAdjustmentManager.orderPropagated(order).run().futureValue.rightVal

      val summary = SellableInventorySummaries.findOneById(sellable.id).run().futureValue.value
      summary.onHand must === (sellable.onHand)
      summary.onHold must === (sellable.onHold - 2)
      summary.reserved must === (sellable.reserved + 2)
      summary.safetyStock must === (sellable.safetyStock)

      val adjustments = InventoryAdjustments.findSellableBySummaryId(sellable.id).result.run().futureValue.value
      adjustments must have size 2
      val afs1 = sellable.availableForSale + 2
      val afs2 = afs1 - 2
      adjustments.map(adj ⇒ (adj.state, adj.change, adj.newAfs, adj.newQuantity)) must contain allOf (
        (OnHold, -2, afs1, sellable.onHold - 2),
        (Reserved, 2, afs2, sellable.reserved + 2))
    }

    "adjusts inventory on WMS override" in new Fixture {
      val newOnHand = sellable.onHand + 111
      val newOnHold = sellable.onHold + 222
      val newReserved = sellable.reserved + 333

      val event = WmsOverride(skuId = product.skuId, warehouseId = warehouse.id,
        onHand = newOnHand,
        onHold = newOnHold,
        reserved = newReserved)
      InventoryAdjustmentManager.wmsOverride(event).run().futureValue.rightVal

      val summary = SellableInventorySummaries.findOneById(sellable.id).run().futureValue.value
      summary.onHand must === (newOnHand)
      summary.onHold must === (newOnHold)
      summary.reserved must === (newReserved)
      summary.safetyStock must === (sellable.safetyStock)

      val adjustments = InventoryAdjustments.findSellableBySummaryId(sellable.id).result.run().futureValue.value
      adjustments must have size 3
      val afs1 = sellable.availableForSale + 111
      val afs2 = afs1 - 222
      val afs3 = afs2 - 333
      adjustments.map(adj ⇒ (adj.state, adj.change, adj.newAfs, adj.newQuantity)) must contain allOf (
        (OnHand, 111, afs1, sellable.onHand + 111),
        (OnHold, 222, afs2, sellable.onHold + 222),
        (Reserved, 333, afs3, sellable.reserved + 333))
    }

    "does not create adjustment for zero change" in new Fixture {
      val event = WmsOverride(skuId = product.skuId, warehouseId = warehouse.id,
        onHand = sellable.onHand, onHold = sellable.onHold, reserved = sellable.reserved)
      InventoryAdjustmentManager.wmsOverride(event).run().futureValue.rightVal

      val summary = SellableInventorySummaries.findOneById(sellable.id).run().futureValue.value
      summary.onHand must === (sellable.onHand)
      summary.onHold must === (sellable.onHold)
      summary.reserved must === (sellable.reserved)
      summary.safetyStock must === (sellable.safetyStock)

      val adjustments = InventoryAdjustments.findSellableBySummaryId(sellable.id).result.run().futureValue.value
      adjustments mustBe empty
    }
  }

  trait Fixture extends InventorySummaryGenerator with ProductSeeds {
    val (product, sellable, warehouse, admin, order) = (for {
      productCtx ← * <~ ProductContexts.mustFindById404(SimpleContext.id)
      products   ← * <~ createProducts
      product    = products._1
      warehouse  ← * <~ Warehouses.create(Factories.warehouse)
      summaries  ← * <~ generateInventory(skuId = product.skuId, warehouseId = warehouse.id)
      order      ← * <~ Orders.create(Factories.cart)
      admin      ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (product, summaries._1, warehouse, admin, order)).run().futureValue.rightVal

    val lineItems = Seq(UpdateLineItemsPayload(sku = product.code, quantity = 2))
    LineItemUpdater.updateQuantitiesOnOrder(admin, order.refNum, lineItems).futureValue.rightVal
  }

}
