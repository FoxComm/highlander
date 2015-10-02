package services

import models._

import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class OrderUpdaterTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val fullOrder = OrderUpdater.createShippingAddressFromAddressId(address.id, order.refNum).futureValue.get
      fullOrder.shippingAddress must not be 'empty
      val orderAddress = fullOrder.shippingAddress.get

      orderAddress.name mustBe address.name
      orderAddress.address1 mustBe address.address1
      orderAddress.address2 mustBe address.address2
      orderAddress.city mustBe address.city
      orderAddress.zip mustBe address.zip
    }

    "Adds a shipping address by creating a new address in the payload" in new Fixture {
      val newAddress = payloads.CreateAddressPayload(name = "Home Office", regionId = 1, address1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")

      val fullOrder = OrderUpdater.createShippingAddressFromPayload(newAddress, order.refNum).futureValue.get
      fullOrder.shippingAddress must not be 'empty
      val orderAddress = fullOrder.shippingAddress.get

      orderAddress.name mustBe newAddress.name
      orderAddress.address1 mustBe newAddress.address1
      orderAddress.address2 mustBe newAddress.address2
      orderAddress.city mustBe newAddress.city
      orderAddress.zip mustBe newAddress.zip
    }

    "Updates a shipping address by referencing an order that already exists" in new UpdateAddressFixture {
      val fullOrder = OrderUpdater.createShippingAddressFromAddressId(newAddress.id, order.refNum).futureValue.get
      fullOrder.shippingAddress must not be 'empty
      val orderAddress = fullOrder.shippingAddress.get

      orderAddress.name mustBe newAddress.name
      orderAddress.address1 mustBe newAddress.address1
      orderAddress.address2 mustBe newAddress.address2
      orderAddress.city mustBe newAddress.city
      orderAddress.zip mustBe newAddress.zip
    }

    "Updates a shipping address by sending fields in the payload" in new UpdateAddressFixture {
      val payload = payloads.UpdateAddressPayload(name = Some("Don Keyhote"))
      val fullOrder = OrderUpdater.updateShippingAddressFromPayload(payload, order.refNum).futureValue.get
      fullOrder.shippingAddress must not be 'empty
      val orderAddress = fullOrder.shippingAddress.get

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
