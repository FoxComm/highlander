package models

import models.inventory._
import models.product.{SimpleProductData, Mvp, ProductContexts, SimpleContext}

import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import util.IntegrationTestBase
import utils.Slick.implicits._

class InventorySummaryIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "InventorySummary" - {
    "Postgres triggers" - {
      def seed(): (Warehouse, SimpleProductData, Order) = (for {
        productContext ← * <~ ProductContexts.create(SimpleContext.create)
        warehouse ← * <~ Warehouses.create(Factories.warehouse)
        product     ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head.copy(price = 5))
        order     ← * <~ Orders.create(Order(id = 0, customerId = 1, productContextId = productContext.id))
      } yield (warehouse, product, order)).runTxn().futureValue.rightVal

      def adjustment(warehouseId: Int, skuId: Int, orderId: Int, reserved: Int = 0) =
        InventoryAdjustments.create(InventoryAdjustment(
          warehouseId = warehouseId,
          skuId = skuId,
          eventId = orderId,
          onHand = 0,
          onHold = 0,
          reserved = reserved)).run().futureValue.rightVal

      "inserts a negative new record if there is none after an insert to InventoryAdjustment" in {
        val (warehouse, product, order) = seed()
        adjustment(warehouse.id, product.skuId, order.id, reserved = -10)
        val summary = InventorySummaries.findBySkuId(warehouse.id, product.skuId).one.run().futureValue.value

        summary.reserved must === (-10)
      }

      "inserts a positive new record if there is none after an insert to InventoryAdjustment" in {
        val (warehouse, product, order) = seed()
        adjustment(warehouse.id, product.skuId, order.id, reserved = 25)
        val summary = InventorySummaries.findBySkuId(warehouse.id, product.skuId).one.run().futureValue.value

        summary.reserved must === (25)
      }

      "updates an existing record after multiple inserts to InventoryAdjustment" in {
        val (warehouse, product, order) = seed()
        List(10, 50, 0, 3, 2, -30, -30).foreach { r ⇒  { adjustment(warehouse.id, product.skuId, order.id, reserved = r) }}

        val summary = InventorySummaries.findBySkuId(warehouse.id, product.skuId).one.run().futureValue.value

        summary.reserved must === (5)
      }
    }
  }
}

