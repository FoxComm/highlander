package services

import models.product.{SimpleContext, ProductContexts}
import models.StoreAdmins
import models.activity.ActivityContext
import models.inventory.adjustment.InventoryAdjustment._
import models.inventory.adjustment.SellableInventoryAdjustments
import models.order._
import models.inventory.summary._
import models.inventory._
import payloads.UpdateLineItemsPayload
import services.inventory.InventoryAdjustmentManager
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.ProductSeeds
import utils.seeds.Seeds.Factories
import utils.seeds.generators._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

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

      val adjustments = SellableInventoryAdjustments.findBySummaryId(sellable.id).result.run().futureValue
      adjustments.map(_.onHandChange).sum must === (0)
      adjustments.map(_.onHoldChange).sum must === (2)
      adjustments.map(_.reservedChange).sum must === (0)
      adjustments.map(_.safetyStockChange).sum must === (0)
    }

    "adjusts inventory on order propagation to WMS" in new Fixture {
      val event = OrderPropagated(skuId = product.skuId, warehouseId = warehouse.id, quantity = 10, orderRef = "x")
      InventoryAdjustmentManager.orderPropagated(event).run().futureValue.rightVal

      val summary = SellableInventorySummaries.findOneById(sellable.id).run().futureValue.value
      summary.onHand must === (sellable.onHand)
      summary.onHold must === (sellable.onHold - 10)
      summary.reserved must === (sellable.reserved + 10)
      summary.safetyStock must === (sellable.safetyStock)

      val adjustment = SellableInventoryAdjustments.findBySummaryId(sellable.id).one.run().futureValue.value
      adjustment.onHandChange must === (0)
      adjustment.onHoldChange must === (-10)
      adjustment.reservedChange must === (10)
      adjustment.safetyStockChange must === (0)
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

      val adjustment = SellableInventoryAdjustments.findBySummaryId(sellable.id).one.run().futureValue.value
      adjustment.onHandChange must === (111)
      adjustment.onHoldChange must === (222)
      adjustment.reservedChange must === (333)
      adjustment.safetyStockChange must === (0)
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
