package services

import models.inventory._
import java.time.Instant

import cats.data.Xor
import models._
import utils.Seeds.Factories
import payloads.{UpdateLineItemsPayload ⇒ Payload}
import util.IntegrationTestBase
import utils.Slick.implicits._

class LineItemUpdaterTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  val lineItems = TableQuery[OrderLineItems]
  val lineItemSkus = TableQuery[OrderLineItemSkus]

  def createSkusAndLinks(num: Int, order: Order): Unit = {
    (Skus.returningId ++= (1 to num).map { i ⇒
      Factories.skus.head.copy(sku = i.toString, price = 5)
    }).run().futureValue

    (OrderLineItemSkus.returningId ++= (1 to num).map {
      id ⇒ OrderLineItemSku(skuId = id, orderId = order.id) }).run().futureValue
  }

  def createLineItems(items: Seq[OrderLineItem]): Unit = {
    val insert = lineItems ++= items
    db.run(insert).futureValue
  }

  def createDefaultWarehouse() : Warehouse = 
    Warehouses.saveNew(Factories.warehouse).run().futureValue

  def createInventory(warehouseId: Int, skuId: Int, onHand: Int = 100): Unit = {
    val summary = InventorySummary(
      id = 0, 
      warehouseId = warehouseId,
      skuId = skuId, 
      onHand = onHand, 
      onHold = 0, 
      reserved = 0, 
      nonSellable = 0,
      safetyStock = 0, 
      updatedAt = Instant.now())

    InventorySummaries.saveNew(summary).run().futureValue
  }

  "LineItemUpdater" - {

    "Adds line_items when the sku doesn't exist in order" in {
      val order = Orders.create(Order(customerId = 1)).run().futureValue.rightVal
      val warehouse = createDefaultWarehouse()
      createDefaultWarehouse()
      createSkusAndLinks(2, order)
      createInventory(warehouse.id, 1, 100)
      createInventory(warehouse.id, 2, 100)

      val payload = Seq[Payload](
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0)
      )

      LineItemUpdater.updateQuantitiesOnOrder(order.refNum, payload).futureValue match {
        case Xor.Right(root) ⇒
          root.lineItems.skus.count(_.sku == "1") must be(3)
          root.lineItems.skus.count(_.sku == "2") must be(0)

          val allRecords = db.run(lineItems.result).futureValue
          root.lineItems.skus.size must === (allRecords.size)

          val allRelations = db.run(lineItemSkus.result).futureValue
          allRelations.size must === (2)

        case Xor.Left(s) ⇒ fail(s.toList.mkString(";"))
      }
    }

    "Updates line_items when the Sku already is in order" in {
      val order = Orders.create(Order(customerId = 1)).run().futureValue.rightVal
      val warehouse = createDefaultWarehouse()
      createSkusAndLinks(3, order)
      createInventory(warehouse.id, 1, 100)
      createInventory(warehouse.id, 2, 100)
      createInventory(warehouse.id, 3, 100)
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { linkId ⇒
        OrderLineItem(id = 0, orderId = 1, originId = linkId, originType = OrderLineItem.SkuItem)
      }
      createLineItems(seedItems)

      val payload = Seq[Payload](
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0),
        Payload(sku = "3", quantity = 1)
      )

      LineItemUpdater.updateQuantitiesOnOrder(order.refNum, payload).futureValue match {
        case Xor.Right(root) ⇒
          root.lineItems.skus.count(_.sku == "1") must be(3)
          root.lineItems.skus.count(_.sku == "2") must be(0)
          root.lineItems.skus.count(_.sku == "3") must be(1)

          val allRecords = db.run(lineItems.result).futureValue
          root.lineItems.skus.size must === (allRecords.size)

          val allRelations = db.run(lineItemSkus.result).futureValue
          allRelations.size must === (2)

        case Xor.Left(s) ⇒ fail(s.toList.mkString(";"))
      }
    }

    // if we've asked for more than available we will "reserve" up to available_on_hand in Skus
    "Adds line_items up to availableOnHand" in (pending)
  }
}
