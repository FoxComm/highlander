package services

import models.cord._
import models.location.Addresses
import models.traits.Originator
import payloads.AddressPayloads._
import services.carts.CartShippingAddressUpdater._
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CartShippingAddressUpdaterTest
    extends IntegrationTestBase
    with TestObjectContext
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val fullCart = createShippingAddressFromAddressId(Originator(storeAdmin),
                                                        address.id,
                                                        Some(cart.refNum)).gimme
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

      val fullCart = createShippingAddressFromPayload(Originator(storeAdmin),
                                                      newAddress,
                                                      Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === (newAddress.name)
      cartAddress.address1 must === (newAddress.address1)
      cartAddress.address2 must === (newAddress.address2)
      cartAddress.city must === (newAddress.city)
      cartAddress.zip must === (newAddress.zip)
    }

    "Updates a shipping address by referencing an order that already exists" in new UpdateAddressFixture {
      val fullCart = createShippingAddressFromAddressId(Originator(storeAdmin),
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
        updateShippingAddressFromPayload(Originator(storeAdmin), payload, Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === ("Don Keyhote")
    }
  }

  trait Fixture extends EmptyCartWithShipAddress_Baked with StoreAdmin_Seed

  trait UpdateAddressFixture extends Fixture {
    val newAddress = Addresses
      .create(Factories.address
            .copy(customerId = customer.id, name = "New Address", isDefaultShipping = false))
      .gimme
  }
}
