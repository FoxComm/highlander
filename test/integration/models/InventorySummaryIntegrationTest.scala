package models

import models.inventory._

import slick.driver.PostgresDriver
import utils.Seeds.Factories
import util.IntegrationTestBase
import utils.Slick.implicits._

class InventorySummaryIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "InventorySummary" - {
    "Postgres triggers" - {
      def seed(): (Warehouse, Sku, Order) = {
        val warehouse = Warehouses.saveNew(Factories.defaultWarehouse).run().futureValue
        val sku = Skus.saveNew(Factories.skus.head.copy(price = 5)).run().futureValue
        val order = Orders.saveNew(Order(id = 0, customerId = 1)).run().futureValue
        (warehouse, sku, order)
      }

      def adjustment(warehouseId: Int, skuId: Int, orderId: Int, reserved: Int = 0) =
        InventoryAdjustments.saveNew(InventoryAdjustment(
          warehouseId = warehouseId, 
          skuId = skuId, 
          eventId = orderId,
          onHand = 0,
          onHold = 0,
          reserved = reserved)).run().futureValue

      "inserts a negative new record if there is none after an insert to InventoryAdjustment" in {
        val (warehouse, sku, order) = seed()
        adjustment(warehouse.id, sku.id, order.id, reserved = -10)
        val summary = InventorySummaries.findBySkuId(warehouse.id, sku.id).one.run().futureValue.value

        summary.reserved must === (-10)
      }

      "inserts a positive new record if there is none after an insert to InventoryAdjustment" in {
        val (warehouse, sku, order) = seed()
        adjustment(warehouse.id, sku.id, order.id, reserved = 25)
        val summary = InventorySummaries.findBySkuId(warehouse.id, sku.id).one.run().futureValue.value

        summary.reserved must === (25)
      }

      "updates an existing record after multiple inserts to InventoryAdjustment" in {
        val (warehouse, sku, order) = seed()
        List(10, 50, 0, 3, 2, -30, -30).foreach { r ⇒  { adjustment(warehouse.id, sku.id, order.id, reserved = r) }}

        val summary = InventorySummaries.findBySkuId(warehouse.id, sku.id).one.run().futureValue.value

        summary.reserved must === (5)
      }
    }
  }
}

