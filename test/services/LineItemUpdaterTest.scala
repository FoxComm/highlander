package services

import models.{Order, OrderLineItem, OrderLineItems, Orders}
import org.scalactic.{Bad, Good}
import payloads.{UpdateLineItemsPayload ⇒ Payload}
import util.IntegrationTestBase

class LineItemUpdaterTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  val lineItems = TableQuery[OrderLineItems]

  def createLineItems(items: Seq[OrderLineItem]): Unit = {
    val insert = lineItems ++= items
    db.run(insert).futureValue
  }

  "LineItemUpdater" - {

    "Adds line_items when the sku doesn't exist in order" in {
      val order = Orders.save(Order(customerId = 1)).run().futureValue
      val payload = Seq[Payload](
        Payload(skuId = 1, quantity = 3),
        Payload(skuId = 2, quantity = 0)
      )

      LineItemUpdater.updateQuantities(order, payload).futureValue match {
        case Good(items) =>
          items.count(_.skuId == 1) must be(3)
          items.count(_.skuId == 2) must be(0)

          val allRecords = db.run(lineItems.result).futureValue

          items must contain theSameElementsAs allRecords

        case Bad(s) => fail(s.mkString(";"))
      }
    }

    "Updates line_items when the Sku already is in order" in {
      val order = Orders.save(Order(customerId = 1)).run().futureValue
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { skuId => OrderLineItem(id = 0, orderId = 1, skuId = skuId) }
      createLineItems(seedItems)

      val payload = Seq[Payload](
        Payload(skuId = 1, quantity = 3),
        Payload(skuId = 2, quantity = 0),
        Payload(skuId = 3, quantity = 1)
      )

      LineItemUpdater.updateQuantities(order, payload).futureValue match {
        case Good(items) =>
          items.count(_.skuId == 1) must be(3)
          items.count(_.skuId == 2) must be(0)
          items.count(_.skuId == 3) must be(1)

          val allRecords = db.run(lineItems.result).futureValue

          items must contain theSameElementsAs allRecords

        case Bad(s) => fail(s.mkString(";"))
      }
    }
  }
}
