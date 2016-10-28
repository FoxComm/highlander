import java.time.Instant

import failures.NotFoundFailure404
import models.account._
import models.cord.OrderShippingAddresses
import models.customer._
import models.location.{Address, Addresses}
import payloads.AddressPayloads.CreateAddressPayload
import responses.AddressResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class AddressesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with PhoenixAdminApi
    with BakedFixtures {

  "GET /v1/customers/:customerId/addresses" - {
    "lists addresses" in new CustomerAddress_Baked {
      val addresses = customersApi(customer.accountId).addresses.get().as[Seq[AddressResponse]]

      addresses must have size 1
      addresses.head.name must === (address.name)
    }
  }

  "POST /v1/customers/:customerId/addresses" - {
    "creates an address" in new Customer_Seed {
      val payload = CreateAddressPayload(name = "Home Office",
                                         regionId = 1,
                                         address1 = "3000 Coolio Dr",
                                         city = "Seattle",
                                         zip = "55555")
      val newAddress =
        customersApi(customer.accountId).addresses.create(payload).as[AddressResponse]
      newAddress.name must === (payload.name)
      newAddress.isDefault must === (Some(false))
    }
  }

  "POST /v1/customers/:customerId/addresses/:addressId/default" - {
    "sets the isDefaultShippingAddress flag on an address" in new NoDefaultAddressFixture {
      customersApi(customer.accountId).address(address.id).setDefault().mustBeOk()
      Addresses.findOneById(address.id).gimme.value.isDefaultShipping mustBe true
    }

    "sets a new shipping address if there's already a default shipping address" in new CustomerAddress_Baked {
      val another = Addresses.create(address.copy(id = 0, isDefaultShipping = false)).gimme
      customersApi(customer.accountId).address(another.id).setDefault().mustBeOk()

      Addresses.findOneById(another.id).gimme.value.isDefaultShipping mustBe true
      Addresses.findOneById(address.id).gimme.value.isDefaultShipping mustBe false
    }
  }

  "DELETE /v1/customers/:customerId/addresses/default" - {
    "removes an existing default from a shipping address" in new CustomerAddress_Baked {
      customersApi(customer.accountId).addresses.unsetDefault().mustBeEmpty()

      Addresses.findOneById(address.id).gimme.value.isDefaultShipping mustBe false
    }

    "attempts to removes default shipping address when none is set" in new Customer_Seed {
      customersApi(customer.accountId).addresses.unsetDefault().mustBeEmpty()

      Addresses.findAllByAccountId(customer.accountId).length.gimme must === (0)
    }
  }

  "PATCH /v1/customers/:customerId/addresses/:addressId" - {
    "can be edited" in new CustomerAddress_Baked {
      val payload = CreateAddressPayload(name = "Home Office",
                                         regionId = 1,
                                         address1 = "3000 Coolio Dr",
                                         city = "Seattle",
                                         zip = "55555")
      (payload.name, payload.address1) must !==((address.name, address.address1))

      val updated =
        customersApi(customer.accountId).address(address.id).edit(payload).as[AddressResponse]

      (updated.name, updated.address1) must === ((payload.name, payload.address1))
    }
  }

  "DELETE /v1/customers/:customerId/addresses/:addressId" - {
    "can be deleted" in new CustomerAddress_Baked {

      //notice the payload is a default shipping address. Delete should make it not default.
      val payload = CreateAddressPayload(name = "Delete Me",
                                         regionId = 1,
                                         address1 = "5000 Delete Dr",
                                         city = "Deattle",
                                         zip = "666",
                                         isDefault = true)

      val newAddress: AddressResponse =
        customersApi(customer.accountId).addresses.create(payload).as[AddressResponse]

      customersApi(customer.accountId).address(newAddress.id).delete().mustBeEmpty()

      val deletedAddress: Address = Addresses.findOneById(newAddress.id).gimme.value
      deletedAddress.isDefaultShipping mustBe false
      deletedAddress.deletedAt mustBe defined
    }

    "deleted address should be visible to StoreAdmin" in new DeletedAddressFixture {
      customersApi(account.id).address(address.id).get().mustBeOk()
    }

    "deleted address should be invisible to Customer" in new DeletedAddressFixture {
      GET(s"v1/my/addresses/${address.id}")
        .mustFailWith404(NotFoundFailure404(Address, address.id))
    }

    "fails deleting using wrong address id" in new CustomerAddress_Baked {
      customersApi(customer.accountId)
        .address(65536)
        .delete()
        .mustFailWith404(NotFoundFailure404(Address, 65536))
    }

    "fails deleting using wrong customer id" in new CustomerAddress_Baked {
      customersApi(65536)
        .address(address.id)
        .delete()
        .mustFailWith404(NotFoundFailure404(User, 65536))
    }
  }

  "GET /v1/my/addresses" - {
    "retrieves a customer's addresses" in new CustomerAddress_Baked {
      val addresses = GET(s"v1/my/addresses").as[Seq[AddressResponse]]

      addresses must have size 1
      addresses.head.name must === (address.name)
    }
  }

  trait DeletedAddressFixture extends StoreAdmin_Seed {
    val (account, address) = (for {
      accountPre1 ← * <~ Accounts.create(Account())
      accountPre2 ← * <~ Accounts.create(Account())
      account     ← * <~ Accounts.create(Account())
      user        ← * <~ Users.create(Factories.customer.copy(accountId = account.id))
      custData ← * <~ CustomersData.create(
                    CustomerData(userId = user.id, accountId = account.id, scope = Scope.current))
      address ← * <~ Addresses.create(
                   Factories.address.copy(accountId = account.id,
                                          isDefaultShipping = false,
                                          deletedAt = Some(Instant.now)))
    } yield (account, address)).gimme
  }

  trait ShippingAddressFixture extends EmptyCartWithShipAddress_Baked

  trait NoDefaultAddressFixture extends CustomerAddress_Baked with EmptyCustomerCart_Baked {
    val shippingAddress = OrderShippingAddresses.copyFromAddress(address, cart.refNum).gimme
  }
}
