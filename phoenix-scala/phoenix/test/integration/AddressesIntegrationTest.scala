import cats.implicits._
import core.failures.NotFoundFailure404
import phoenix.failures.AddressFailures.NoRegionFound
import phoenix.models.account._
import phoenix.models.cord.OrderShippingAddresses
import phoenix.models.location.{Address, Addresses, Country, Region}
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.responses.AddressResponse
import phoenix.responses.PublicResponses.CountryWithRegions
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixPublicApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.{randomAddress, ApiFixtureHelpers}

class AddressesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with DefaultJwtAdminAuth
    with ApiFixtureHelpers
    with PhoenixAdminApi
    with PhoenixPublicApi
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

      val states = countryWithRegions.regions.map { region ⇒
        (region.abbreviation, region.name)
      }

      states must contain("CA".some, "California")
      states must contain("CO".some, "Colorado")
      states must contain("DE".some, "Delaware")
    }

    "Should not contain absent or non-existent regions" in {
      val countryWithRegions =
        publicApi.getCountryById(Country.unitedStatesId).as[CountryWithRegions]

      val states = countryWithRegions.regions.map { region ⇒
        (region.abbreviation, region.name)
      }

      states mustNot contain("MSK".some, "Moscow")
      states mustNot contain("MO".some, "Moscow Oblast")
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
}
