package models

import slick.driver.PostgresDriver
import utils.Seeds.Factories
import util.IntegrationTestBase
import utils.Slick.implicits._

class InventoryAdjustmentTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  def seed(): (Sku, Order) = {
    val sku = Skus.save(Factories.skus.head.copy(price = 5)).run().futureValue
    val order = Orders.save(Order(id = 0, customerId = 1)).run().futureValue
    (sku, order)
  }

  "InventoryAdjustment" - {
    "createAdjustmentsForOrder creates an adjustment with the correct reservation based on line items" in {
      val (sku, order) = seed()
      (OrderLineItems.returningId ++= (1 to 5).map { _ ⇒
        OrderLineItem(orderId = order.id, originId = sku.id, originType = OrderLineItem.SkuItem)
      }).run().futureValue

      InventoryAdjustments.createAdjustmentsForOrder(order).futureValue
      val summary = InventorySummaries.findBySkuId(sku.id).futureValue.get
      val numAdjustments = InventoryAdjustments.filter(_.inventoryEventId === order.id).length.result.run().futureValue

      numAdjustments mustBe 1
      summary.availableOnHand must === (-5)
    }
  }
}

