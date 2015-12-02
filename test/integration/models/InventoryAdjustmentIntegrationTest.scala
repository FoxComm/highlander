package models

import models.inventory._
import utils.seeds.Seeds
import Seeds.Factories
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

class InventoryAdjustmentIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  def seed(): (Warehouse, Sku, OrderLineItemSku, Order) = (for {
    warehouse   ← * <~ Warehouses.create(Factories.warehouse)
    sku         ← * <~ Skus.create(Factories.skus.head.copy(price = 5))
    order       ← * <~ Orders.create(Order(id = 0, customerId = 1))
    lineItemSku ← * <~ OrderLineItemSkus.create(OrderLineItemSku(skuId = sku.id, orderId = order.id))
  } yield (warehouse, sku, lineItemSku, order)).runT().futureValue.rightVal

  "InventoryAdjustment" - {
    "createAdjustmentsForOrder creates an adjustment with the correct reservation based on line items" in {
      // Simulate `order_line_item_skus` offset, to make SKU ID different from relation ID
      // This is required to properly test query in `InventoryAdjustment.createAdjustmentsForOrder()`
      seed()
      Orders.findByCustomerId(1).map(_.status).update(Order.Shipped).run().futureValue

      // Start actual testing
      val (warehouse, sku, lineItemSku, order) = seed()

      (OrderLineItems.returningId ++= (1 to 5).map { _ ⇒
        OrderLineItem(orderId = order.id, originId = lineItemSku.id, originType = OrderLineItem.SkuItem)
      }).run().futureValue

      InventoryAdjustments.createAdjustmentsForOrder(order, warehouse.id).run().futureValue
      val numAdjustments = InventoryAdjustments.filter(_.eventId === order.id).length.result.run().futureValue
      val summary = InventorySummaries.findBySkuId(warehouse.id, sku.id).one.run().futureValue.value

      numAdjustments mustBe 1
      summary.reserved must === (5)
    }
  }
}

