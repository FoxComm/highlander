package services

import cats.data.Xor
import models.{Order, OrderLineItem, OrderLineItems, Orders, InventorySummaries, InventorySummary, Skus, Sku}
import utils.Seeds.Factories
import payloads.{UpdateLineItemsPayload ⇒ Payload}
import util.IntegrationTestBase
import utils.Slick.implicits._

class LineItemUpdaterTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  val lineItems = TableQuery[OrderLineItems]

  def createSkus(num: Int): Unit =
    (Skus.returningId ++= (1 to num).map { i ⇒
      Factories.skus.head.copy(sku = i.toString, price = 5)
    }).run().futureValue

  def createLineItems(items: Seq[OrderLineItem]): Unit = {
    val insert = lineItems ++= items
    db.run(insert).futureValue
  }

  def createInventory(skuId: Int, availableOnHand: Int = 100): Unit = {
    val summary = InventorySummary(id = 0, skuId = skuId, availableOnHand = availableOnHand, availablePreOrder = 0,
                                   availableBackOrder = 0, outstandingPreOrders = 0, outstandingBackOrders = 0)
    InventorySummaries.save(summary).run().futureValue
  }

  "LineItemUpdater" - {

    "Adds line_items when the sku doesn't exist in order" in {
      createSkus(2)
      val order = Orders.save(Order(customerId = 1)).run().futureValue
      createInventory(1, 100)
      createInventory(2, 100)

      val payload = Seq[Payload](
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0)
      )

      LineItemUpdater.updateQuantities(order, payload).futureValue match {
        case Xor.Right(root) =>
          root.lineItems.count(_.sku == "1") must be(3)
          root.lineItems.count(_.sku == "2") must be(0)

          val allRecords = db.run(lineItems.result).futureValue
          root.lineItems.size must === (allRecords.size)

        case Xor.Left(s) => fail(s.mkString(";"))
      }
    }

    "Updates line_items when the Sku already is in order" in {
      createSkus(3)
      val order = Orders.save(Order(customerId = 1)).run().futureValue
      createInventory(1, 100)
      createInventory(2, 100)
      createInventory(3, 100)
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { skuId =>
        OrderLineItem(id = 0, orderId = 1, skuId = skuId)
      }
      createLineItems(seedItems)

      val payload = Seq[Payload](
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0),
        Payload(sku = "3", quantity = 1)
      )

      LineItemUpdater.updateQuantities(order, payload).futureValue match {
        case Xor.Right(root) =>
          root.lineItems.count(_.sku == "1") must be(3)
          root.lineItems.count(_.sku == "2") must be(0)
          root.lineItems.count(_.sku == "3") must be(1)

          val allRecords = db.run(lineItems.result).futureValue

          root.lineItems.size must === (allRecords.size)

        case Xor.Left(s) => fail(s.mkString(";"))
      }
    }

    // if we've asked for more than available we will "reserve" up to available_on_hand in Skus
    "Adds line_items up to availableOnHand" in (pending)
  }
}
