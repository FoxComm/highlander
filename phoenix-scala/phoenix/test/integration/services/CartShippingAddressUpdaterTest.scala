package services

import cats.implicits._
import core.failures.NotFoundFailure404
import phoenix.models.location.{Address, Addresses}
import phoenix.payloads.AddressPayloads._
import phoenix.services.carts.CartShippingAddressUpdater._
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.BakedFixtures

class CartShippingAddressUpdaterTest
    extends IntegrationTestBase
    with TestObjectContext
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val fullCart =
        createShippingAddressFromAddressId(storeAdmin, address.id, Some(cart.refNum)).gimme
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
        createShippingAddressFromPayload(storeAdmin, newAddress, Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === (newAddress.name)
      cartAddress.address1 must === (newAddress.address1)
      cartAddress.address2 must === (newAddress.address2)
      cartAddress.city must === (newAddress.city)
      cartAddress.zip must === (newAddress.zip)
    }

    "Updates a shipping address by referencing an order that already exists" in new UpdateAddressFixture {
      val fullCart =
        createShippingAddressFromAddressId(storeAdmin, newAddress.id, Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === (newAddress.name)
      cartAddress.address1 must === (newAddress.address1)
      cartAddress.address2 must === (newAddress.address2)
      cartAddress.city must === (newAddress.city)
      cartAddress.zip must === (newAddress.zip)
    }

    "Updates a shipping address by sending fields in the payload" in new UpdateAddressFixture {
      val payload  = UpdateAddressPayload(name = Some("Don Keyhote"))
      val fullCart = updateShippingAddressFromPayload(storeAdmin, payload, Some(cart.refNum)).gimme
      fullCart.result.shippingAddress must not be 'empty
      val cartAddress = fullCart.result.shippingAddress.value

      cartAddress.name must === ("Don Keyhote")
    }

    "Delete existing shipping address that was updated" in new UpdateAddressFixture {
      val api     = cartsApi(cart.refNum).shippingAddress
      val payload = UpdateAddressPayload(name = faker.Name.name.some)
      api.update(payload).mustBeOk()

      api.delete().mustBeOk()

      api.update(payload).mustFailWith404(NotFoundFailure404(Address, cart.refNum))
    }
  }

  trait Fixture extends EmptyCartWithShipAddress_Baked with StoreAdmin_Seed

  trait UpdateAddressFixture extends Fixture {
    val newAddress = Addresses
      .create(
        Factories.address.copy(accountId = customer.accountId,
                               name = customer.name.getOrElse(faker.Name.name),
                               isDefaultShipping = false))
      .gimme
  }
}
