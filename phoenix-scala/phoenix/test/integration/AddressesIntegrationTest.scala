import cats.implicits._
import core.failures.NotFoundFailure404
import phoenix.failures.AddressFailures.NoRegionFound
import phoenix.models.account._
import phoenix.models.cord.OrderShippingAddresses
import phoenix.models.location.{Address, Addresses, Country, Region}
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.responses.AddressResponse
import phoenix.responses.PublicResponses.CountryWithRegions
import phoenix.responses.cord.CartResponse
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixPublicApi, PhoenixStorefrontApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.{ApiFixtureHelpers, randomAddress}

class AddressesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with DefaultJwtAdminAuth
    with ApiFixtureHelpers
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with PhoenixPublicApi
    with BakedFixtures {

  "GET /v1/customers/:customerId/addresses" - {
    pending
    "lists addresses" in new CustomerAddress_Baked {
      val addresses = customersApi(customer.accountId).addresses.get().as[Seq[AddressResponse]]

      addresses must have size 1
      addresses.head.name must === (address.name)
    }
  }

  "POST /v1/customers/:customerId/addresses" - {
    pending
    "creates an address" in new Customer_Seed with AddressFixtures {
      val newAddress =
        customersApi(customer.accountId).addresses.create(addressPayload).as[AddressResponse]
      newAddress.name must === (addressPayload.name)
      newAddress.isDefault must === (Some(false))
    }
  }

  "POST /v1/customers/:customerId/addresses/:addressId/default" - {
    pending
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
    pending
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
    pending
    "can be edited" in new CustomerAddress_Baked with AddressFixtures {
      (addressPayload.name, addressPayload.address1) must !==((address.name, address.address1))

      val updated = customersApi(customer.accountId)
        .address(address.id)
        .edit(addressPayload)
        .as[AddressResponse]

      (updated.name, updated.address1) must === ((addressPayload.name, addressPayload.address1))
    }
  }

  "DELETE /v1/customers/:customerId/addresses/:addressId" - {
    pending
    "can be deleted" in new CustomerAddress_Baked with AddressFixtures {

      //notice the payload is a default shipping address. Delete should make it not default.
      val payload = addressPayload.copy(isDefault = true)

      val newAddress: AddressResponse =
        customersApi(customer.accountId).addresses.create(payload).as[AddressResponse]

      customersApi(customer.accountId).address(newAddress.id).delete().mustBeEmpty()

      val deletedAddress: Address = Addresses.findOneById(newAddress.id).gimme.value
      deletedAddress.isDefaultShipping mustBe false
      deletedAddress.deletedAt mustBe defined
    }

    "deleted address should be visible to StoreAdmin" in {
      val customer = api_newCustomer()
      val address  = customersApi(customer.id).addresses.create(randomAddress()).as[AddressResponse]
      customersApi(customer.id).address(address.id).delete().mustBeEmpty()

      customersApi(customer.id).address(address.id).get().mustBeOk()
    }

    "deleted address should be invisible to Customer" in {
      val (customer, loginData) = api_newCustomerWithLogin()
      val address               = customersApi(customer.id).addresses.create(randomAddress()).as[AddressResponse]
      customersApi(customer.id).address(address.id).delete().mustBeEmpty()

      withCustomerAuth(loginData, customer.id) { implicit auth ⇒
        GET(s"v1/my/addresses/${address.id}", auth.jwtCookie.some)
          .mustFailWith404(NotFoundFailure404(Address, address.id))
      }
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

  "PUT /v1/my/addresses" - {
    "put shipping addresses in" in new AddressFixtures {
      withNewCustomerAuth(TestLoginData.random) { implicit auth ⇒
        cartsApi.create(CreateCart(customerId = auth.customerId.some)).as[CartResponse]
        storefrontCartsApi.shippingAddress.createOrUpdate(addressPayload).mustBeOk()
      }
    }
  }

  "GET /v1/my/addresses" - {
    "retrieves a customer's addresses" in {
      val (customer, loginData) = api_newCustomerWithLogin()
      val address               = customersApi(customer.id).addresses.create(randomAddress()).as[AddressResponse]

      withCustomerAuth(loginData, customer.id) { implicit auth ⇒
        GET(s"v1/my/addresses", auth.jwtCookie.some)
          .as[Seq[AddressResponse]]
          .onlyElement
          .id must === (address.id)
      }
    }
  }

  "GET country by id" - {
    "Make sure that we have region short name provided" in {
      val countryWithRegions =
        publicApi.getCountryById(Country.unitedStatesId).as[CountryWithRegions]
      countryWithRegions.country.id must === (Country.unitedStatesId)
      countryWithRegions.country.alpha2 must === ("US")

      countryWithRegions.regions.map { region ⇒
        (region.abbreviation, region.name)
      } must contain
      theSameElementsAs(
          List(
              ("CA".some, "California"),
              ("CO".some, "Colorado"),
              ("DE".some, "Delaware")
          ))
    }

    "Should not contain absent or non-existent regions" in {
      val countryWithRegions =
        publicApi.getCountryById(Country.unitedStatesId).as[CountryWithRegions]

      countryWithRegions.regions.map { region ⇒
        (region.abbreviation, region.name)
      } mustNot contain
      theSameElementsAs(
          List(
              ("MSK".some, "Moscow"),
              ("MO".some, "Moscow Oblast")
          ))
    }
  }

  "GET region by short name" - {
    "Must return existed region for a given short name" in {
      publicApi.getRegionByShortName("CO").as[Region].name must === ("Colorado")
    }

    "Make sure that it works for lower case input" in {
      publicApi.getRegionByShortName("mo").as[Region].name must === ("Missouri")
    }

    "Should not contain absent or non-existent regions" in {
      publicApi.getRegionByShortName("xx").mustFailWith400(NoRegionFound("xx"))
    }

  }

  trait ShippingAddressFixture extends EmptyCartWithShipAddress_Baked

  trait NoDefaultAddressFixture extends CustomerAddress_Baked with EmptyCustomerCart_Baked {
    val shippingAddress = OrderShippingAddresses.copyFromAddress(address, cart.refNum).gimme
  }

  trait AddressFixtures {
    val addressPayload = CreateAddressPayload(name = "Home Office",
                                              regionId = 1,
                                              address1 = "3000 Coolio Dr",
                                              city = "Seattle",
                                              zip = "55555")
  }

}
