package models

import util.IntegrationTestBase

class InventorySummaryTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "InventorySummary" - {
    "Postgres triggers" - {
      def seed(reserved: Int): (Sku, Order, InventoryAdjustment) = {
        val sku = Skus.save(Sku(price = 5)).run().futureValue
        val order = Orders.save(Order(id = 0, customerId = 1)).run().futureValue
        val adjustment = InventoryAdjustments.save(InventoryAdjustment(skuId = sku.id, inventoryEventId = order.id,
          reservedForFulfillment = reserved)).run().futureValue

        (sku, order, adjustment)
      }

      "inserts a new record if there is none after an insert to InventoryAdjustment" - {
        val (sku, _, _) = seed(10)
        val summary = db.run(InventorySummaries._findBySkuId(sku.id).result.head).futureValue

        summary.availableOnHand mustBe (-10)
      }

      "updates an existing record after insert to InventoryAdjustment" - {
        val (sku, order, adjustment) = seed(10)
        InventoryAdjustments.save(InventoryAdjustment(skuId = sku.id, inventoryEventId = order.id,
          reservedForFulfillment = 5)).run().futureValue
        val summary = db.run(InventorySummaries._findBySkuId(sku.id).result.head).futureValue

        summary.availableOnHand mustBe (-15)
      }
    }
  }
}

