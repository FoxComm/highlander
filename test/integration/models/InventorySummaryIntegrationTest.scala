package models

import slick.driver.PostgresDriver
import utils.Seeds.Factories
import util.IntegrationTestBase
import utils.Slick.implicits._

class InventorySummaryIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "InventorySummary" - {
    "Postgres triggers" - {
      def seed(): (Sku, Order) = {
        val sku = Skus.saveNew(Factories.skus.head.copy(price = 5)).run().futureValue
        val order = Orders.saveNew(Order(id = 0, customerId = 1)).run().futureValue
        (sku, order)
      }

      def adjustment(skuId: Int, orderId: Int, reserved: Int): PostgresDriver.api.DBIO[InventoryAdjustment] =
        InventoryAdjustments.saveNew(InventoryAdjustment(skuId = skuId, inventoryEventId = orderId,
          reservedForFulfillment = reserved))

      "inserts a negative new record if there is none after an insert to InventoryAdjustment" in {
        val (sku, order) = seed()
        adjustment(sku.id, order.id, reserved = 10).run().futureValue
        val summary = InventorySummaries.findBySkuId(sku.id).one.run().futureValue.value

        summary.availableOnHand must === (-10)
      }

      "inserts a positive new record if there is none after an insert to InventoryAdjustment" in {
        val (sku, order) = seed()
        adjustment(sku.id, order.id, reserved = -25).run().futureValue
        val summary = InventorySummaries.findBySkuId(sku.id).one.run().futureValue.value

        summary.availableOnHand must === (25)
      }

      "updates an existing record after multiple inserts to InventoryAdjustment" in {
        val (sku, order) = seed()
        List(10, 50, 0, 3, 2, -30, -30).foreach { r ⇒ adjustment(sku.id, order.id, reserved = r).run().futureValue }

        val summary = InventorySummaries.findBySkuId(sku.id).one.run().futureValue.value

        summary.availableOnHand must === (-5)
      }
    }
  }
}

