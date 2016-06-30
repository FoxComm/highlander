package services

import scala.concurrent.ExecutionContext.Implicits.global

import models.StoreAdmins
import models.customer.Customers
import models.location.Addresses
import models.order._
import models.traits.Originator
import payloads.AddressPayloads._
import services.orders.OrderShippingAddressUpdater._
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class OrderShippingAddressUpdaterTest
    extends IntegrationTestBase
    with TestActivityContext.AdminAC {

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val fullOrder =
        createShippingAddressFromAddressId(Originator(admin), address.id, Some(order.refNum)).gimme
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must === (address.name)
      orderAddress.address1 must === (address.address1)
      orderAddress.address2 must === (address.address2)
      orderAddress.city must === (address.city)
      orderAddress.zip must === (address.zip)
    }

    "Adds a shipping address by creating a new address in the payload" in new Fixture {
      val newAddress = CreateAddressPayload(name = "Home Office",
                                            regionId = 1,
                                            address1 = "3000 Coolio Dr",
                                            city = "Seattle",
                                            zip = "55555")

      val fullOrder =
        createShippingAddressFromPayload(Originator(admin), newAddress, Some(order.refNum)).gimme
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must === (newAddress.name)
      orderAddress.address1 must === (newAddress.address1)
      orderAddress.address2 must === (newAddress.address2)
      orderAddress.city must === (newAddress.city)
      orderAddress.zip must === (newAddress.zip)
    }

    "Updates a shipping address by referencing an order that already exists" in new UpdateAddressFixture {
      val fullOrder = createShippingAddressFromAddressId(Originator(admin),
                                                         newAddress.id,
                                                         Some(order.refNum)).gimme
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must === (newAddress.name)
      orderAddress.address1 must === (newAddress.address1)
      orderAddress.address2 must === (newAddress.address2)
      orderAddress.city must === (newAddress.city)
      orderAddress.zip must === (newAddress.zip)
    }

    "Updates a shipping address by sending fields in the payload" in new UpdateAddressFixture {
      val payload = UpdateAddressPayload(name = Some("Don Keyhote"))
      val fullOrder =
        updateShippingAddressFromPayload(Originator(admin), payload, Some(order.refNum)).gimme
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must === ("Don Keyhote")
    }
  }

  trait Fixture {
    val (admin, customer, address, order) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      address  ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      order ← * <~ Orders.create(
                 Factories.order.copy(customerId = customer.id, state = Order.Cart))
    } yield (admin, customer, address, order)).gimme
  }

  trait UpdateAddressFixture extends Fixture {
    val (orderShippingAddress, newAddress) = (for {
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                         orderRef = order.refNum)
      newAddress ← * <~ Addresses.create(
                      Factories.address.copy(customerId = customer.id,
                                             name = "New Address",
                                             isDefaultShipping = false))
    } yield (orderShippingAddress, newAddress)).gimme
  }
}
