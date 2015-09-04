package services

import models._

import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class OrderUpdaterTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val payload = payloads.CreateShippingAddress(Some(address.id), None)
      val orderAddress = OrderUpdater.createShippingAddress(order, payload).futureValue.get

      orderAddress.name mustBe address.name
      orderAddress.street1 mustBe address.street1
      orderAddress.street2 mustBe address.street2
      orderAddress.city mustBe address.city
      orderAddress.zip mustBe address.zip
    }

    "Adds a shipping address by creating a new address in the payload" in new Fixture {
      val newAddress = payloads.CreateAddressPayload(name = "Home Office", regionId = 1, street1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      val payload = payloads.CreateShippingAddress(None, Some(newAddress))

      val orderAddress = OrderUpdater.createShippingAddress(order, payload).futureValue.get

      orderAddress.name mustBe newAddress.name
      orderAddress.street1 mustBe newAddress.street1
      orderAddress.street2 mustBe newAddress.street2
      orderAddress.city mustBe newAddress.city
      orderAddress.zip mustBe newAddress.zip
    }

    "Updates a shipping address by referencing an order that already exists" in new UpdateAddressFixture {
      val payload = payloads.UpdateShippingAddress(Some(newAddress.id), None)
      val orderAddress = OrderUpdater.updateShippingAddress(order, payload).futureValue.get

      orderAddress.name mustBe newAddress.name
      orderAddress.street1 mustBe newAddress.street1
      orderAddress.street2 mustBe newAddress.street2
      orderAddress.city mustBe newAddress.city
      orderAddress.zip mustBe newAddress.zip
    }

    "Updates a shipping address by sending fields in the payload" in new UpdateAddressFixture {
      val updateAddress = payloads.UpdateAddressPayload(name = Some("Don Keyhote"))
      val payload = payloads.UpdateShippingAddress(None, Some(updateAddress))
      val orderAddress = OrderUpdater.updateShippingAddress(order, payload).futureValue.get

      orderAddress.name mustBe "Don Keyhote"
    }
  }

  trait Fixture {
    val (customer, address, order) = (for {
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
    } yield (customer, address, order)).run().futureValue
  }

  trait UpdateAddressFixture extends Fixture {
    val (orderShippingAddress, newAddress) = (for {
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
      newAddress ← Addresses.save(Factories.address.copy(customerId = customer.id, name = "New Address",
        isDefaultShipping = false))
    } yield (orderShippingAddress, newAddress)).run().futureValue
  }
}
