package services

import scala.concurrent.ExecutionContext.Implicits.global

import models.StoreAdmins
import models.cord._
import models.customer.Customers
import models.location.Addresses
import models.traits.Originator
import payloads.AddressPayloads._
import services.carts.CartShippingAddressUpdater._
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class CartShippingAddressUpdaterTest
    extends IntegrationTestBase
    with TestObjectContext
    with TestActivityContext.AdminAC {

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val fullCart =
        createShippingAddressFromAddressId(Originator(admin), address.id, Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === (address.name)
      cartAddress.address1 must === (address.address1)
      cartAddress.address2 must === (address.address2)
      cartAddress.city must === (address.city)
      cartAddress.zip must === (address.zip)
    }

    "Adds a shipping address by creating a new address in the payload" in new Fixture {
      val newAddress = CreateAddressPayload(name = "Home Office",
                                            regionId = 1,
                                            address1 = "3000 Coolio Dr",
                                            city = "Seattle",
                                            zip = "55555")

      val fullCart =
        createShippingAddressFromPayload(Originator(admin), newAddress, Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === (newAddress.name)
      cartAddress.address1 must === (newAddress.address1)
      cartAddress.address2 must === (newAddress.address2)
      cartAddress.city must === (newAddress.city)
      cartAddress.zip must === (newAddress.zip)
    }

    "Updates a shipping address by referencing an order that already exists" in new UpdateAddressFixture {
      val fullCart = createShippingAddressFromAddressId(Originator(admin),
                                                        newAddress.id,
                                                        Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === (newAddress.name)
      cartAddress.address1 must === (newAddress.address1)
      cartAddress.address2 must === (newAddress.address2)
      cartAddress.city must === (newAddress.city)
      cartAddress.zip must === (newAddress.zip)
    }

    "Updates a shipping address by sending fields in the payload" in new UpdateAddressFixture {
      val payload = UpdateAddressPayload(name = Some("Don Keyhote"))
      val fullCart =
        updateShippingAddressFromPayload(Originator(admin), payload, Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === ("Don Keyhote")
    }
  }

  trait Fixture {
    val (admin, customer, address, cart) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      address  ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      cart     ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
    } yield (admin, customer, address, cart)).gimme
  }

  trait UpdateAddressFixture extends Fixture {
    val (orderShippingAddress, newAddress) = (for {
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                         cordRef = cart.refNum)
      newAddress ← * <~ Addresses.create(
                      Factories.address.copy(customerId = customer.id,
                                             name = "New Address",
                                             isDefaultShipping = false))
    } yield (orderShippingAddress, newAddress)).gimme
  }
}
