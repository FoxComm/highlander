package models

import slick.driver.PostgresDriver
import util.IntegrationTestBase

class InventorySummaryTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "InventorySummary" - {
    "Postgres triggers" - {
      def seed(): (Sku, Order) = {
        val sku = Skus.save(Sku(price = 5)).run().futureValue
        val order = Orders.save(Order(id = 0, customerId = 1)).run().futureValue
        (sku, order)
      }

      def adjustment(skuId: Int, orderId: Int, reserved: Int): PostgresDriver.api.DBIO[InventoryAdjustment] =
        InventoryAdjustments.save(InventoryAdjustment(skuId = skuId, inventoryEventId = orderId,
          reservedForFulfillment = reserved))

      "inserts a negative new record if there is none after an insert to InventoryAdjustment" in {
        val (sku, order) = seed()
        adjustment(sku.id, order.id, reserved = 10).run()
        val summary = InventorySummaries.findBySkuId(sku.id).futureValue.get

        summary.availableOnHand must === (-10)
      }

      "inserts a positive new record if there is none after an insert to InventoryAdjustment" in {
        val (sku, order) = seed()
        adjustment(sku.id, order.id, reserved = -25).run()
        val summary = InventorySummaries.findBySkuId(sku.id).futureValue.get

        summary.availableOnHand must === (25)
      }

      "updates an existing record after multiple inserts to InventoryAdjustment" in {
        val (sku, order) = seed()
        List(10, 50, 0, 3, 2, -30, -30).foreach { r => adjustment(sku.id, order.id, reserved = r).run().futureValue }

        val summary = InventorySummaries.findBySkuId(sku.id).futureValue.get

        summary.availableOnHand must === (-5)
      }
    }
  }
}

