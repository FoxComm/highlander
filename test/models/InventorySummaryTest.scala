package models

import util.IntegrationTestBase

class InventorySummaryTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "InventorySummary" - {
    "Postgres triggers" - {
      def seed(reserved: Int): (Sku, Order) = {
        val sku = Skus.save(Sku(price = 5)).run().futureValue
        val order = Orders.save(Order(id = 0, customerId = 1)).run().futureValue
        (sku, order)
      }

      def adjustment(skuId: Int, orderId: Int, reserved: Int): InventoryAdjustment = {
        InventoryAdjustments.save(InventoryAdjustment(skuId = skuId, inventoryEventId = orderId,
          reservedForFulfillment = reserved)).run().futureValue
      }

      "inserts a new record if there is none after an insert to InventoryAdjustment" - {
        val (sku, order) = seed(10)
        adjustment(sku.id, order.id, reserved = 10)
        val summary = InventorySummaries.findBySkuId(sku.id).futureValue.get

        summary.availableOnHand mustBe (-10)
      }

      "updates an existing record after insert to InventoryAdjustment" - {
        val (sku, order) = seed(10)
        adjustment(sku.id, order.id, reserved = 10)
        adjustment(sku.id, order.id, reserved = 5)

        val summary = InventorySummaries.findBySkuId(sku.id).futureValue.get

        summary.availableOnHand mustBe (-15)
      }
    }
  }
}

