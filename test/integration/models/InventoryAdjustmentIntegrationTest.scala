package models

import models.inventory._
import models.product.{SimpleProductData, Mvp, ProductContexts, SimpleContext}
import models.order.lineitems._
import models.order.{Orders, Order}
import utils.seeds.Seeds
import Seeds.Factories
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

class InventoryAdjustmentIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  def seed(): (Warehouse, SimpleProductData, OrderLineItemSku, Order) = (for {
    productContext ← * <~ ProductContexts.mustFindById404(SimpleContext.id)
    warehouse   ← * <~ Warehouses.create(Factories.warehouse)
    product     ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head.copy(price = 5))
    order       ← * <~ Orders.create(Order(id = 0, customerId = 1, productContextId = productContext.id))
    lineItemSku ← * <~ OrderLineItemSkus.safeFindBySkuId(product.skuId).toXor
  } yield (warehouse, product, lineItemSku, order)).runTxn().futureValue.rightVal

  "InventoryAdjustment" - {
    "createAdjustmentsForOrder creates an adjustment with the correct reservation based on line items" in {
      // Simulate `order_line_item_skus` offset, to make SKU ID different from relation ID
      // This is required to properly test query in `InventoryAdjustment.createAdjustmentsForOrder()`
      seed()
      Orders.findByCustomerId(1).map(_.state).update(Order.Shipped).run().futureValue

      // Start actual testing
      val (warehouse, product, lineItemSku, order) = seed()

      OrderLineItems.createAllReturningIds((1 to 5).map { _ ⇒
        OrderLineItem(orderId = order.id, originId = lineItemSku.id, originType = OrderLineItem.SkuItem)
      }).run().futureValue

      InventoryAdjustments.createAdjustmentsForOrder(order, warehouse.id).run().futureValue
      val numAdjustments = InventoryAdjustments.filter(_.eventId === order.id).length.result.run().futureValue
      val summary = InventorySummaries.findSellableBySkuIdInWarehouse(warehouse.id, product.skuId).one.run().futureValue.value

      numAdjustments mustBe 1
      summary.reserved must === (5)
    }
  }
}

