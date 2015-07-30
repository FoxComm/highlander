package services

import models._
import org.scalactic.{Bad, Good}
import payloads.{CreateShippingAddress ⇒ Payload}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._

class OrderUpdaterTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  val lineItems = TableQuery[OrderLineItems]

  def createSkus(num: Int): Unit =
    (Skus.returningId ++= (1 to num).map { i ⇒ Sku(price = 5) }).run().futureValue

  def createLineItems(items: Seq[OrderLineItem]): Unit = {
    val insert = lineItems ++= items
    db.run(insert).futureValue
  }

  def createInventory(skuId: Int, availableOnHand: Int = 100): Unit = {
    val summary = InventorySummary(id = 0, skuId = skuId, availableOnHand = availableOnHand, availablePreOrder = 0,
                                   availableBackOrder = 0, outstandingPreOrders = 0, outstandingBackOrders = 0)
    InventorySummaries.save(summary).run().futureValue
  }

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val payload = Payload(Some(address.id), None)
      val orderAddress = OrderUpdater.createShippingAddress(order, payload).futureValue.get

      orderAddress.name mustBe address.name
      orderAddress.street1 mustBe address.street1
      orderAddress.street2 mustBe address.street2
      orderAddress.city mustBe address.city
      orderAddress.zip mustBe address.zip
    }

    "Adds a shipping address by creating a new address in the payload" in new Fixture {
      val newAddress = payloads.CreateAddressPayload(name = "Home Office", stateId = 1, street1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      val payload = Payload(None, Some(newAddress))

      val orderAddress = OrderUpdater.createShippingAddress(order, payload).futureValue.get

      orderAddress.name mustBe newAddress.name
      orderAddress.street1 mustBe newAddress.street1
      orderAddress.street2 mustBe newAddress.street2
      orderAddress.city mustBe newAddress.city
      orderAddress.zip mustBe newAddress.zip
    }
  }

  trait Fixture {
    val (customer, address, order) = (for {
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
    } yield (customer, address, order)).run().futureValue
  }
}
