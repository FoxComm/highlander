package models

import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils._

class GiftCardAdjustmentTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  def seed(): GiftCard = {
    Seeds.Factories.giftCard
    val sku = Skus.save(Sku(price = 5)).run().futureValue
    val order = Orders.save(Order(id = 0, customerId = 1)).run().futureValue
    (sku, order)
  }

  "GiftCardBalanceTest" - {
    "neither credit nor debit can be negative" in {
      val (sku, order) = seed()
      (OrderLineItems.returningId ++= (1 to 5).map { _ ⇒ OrderLineItem(orderId = order.id, skuId = sku.id) }).run()

      InventoryAdjustments.createAdjustmentsForOrder(order).futureValue
      val summary = InventorySummaries.findBySkuId(sku.id).futureValue.get
      val numAdjustments = InventoryAdjustments.filter(_.inventoryEventId === order.id).length.result.run().futureValue

      numAdjustments mustBe (1)
      summary.availableOnHand mustBe (-5)
    }
  }
}

