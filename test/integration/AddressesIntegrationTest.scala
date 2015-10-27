import scala.concurrent.Future
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import models.{Addresses, Customer, Customers, OrderShippingAddresses, Orders, Regions}
import responses.ResponseWithFailuresAndMetadata
import util.IntegrationTestBase
import util.SlickSupport.implicits._
import utils.Seeds.Factories
import utils.Slick.implicits._

class AddressesIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[responses.Addresses.Root]
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import api._
  import org.json4s.jackson.JsonMethods._

  // paging and sorting API
  private var currentCustomer: Customer = _

  override def beforeSortingAndPaging() = {
    currentCustomer = Customers.save(Factories.customer).futureValue
  }

  def uriPrefix = s"v1/customers/${currentCustomer.id}/addresses"

  def responseItems = {
    val items = (1 to numOfResults).map { i ⇒
      val dbio = for {
        address ← Addresses.save(Factories.generateAddress.copy(customerId = currentCustomer.id))
        region  ← Regions.findById(address.regionId).result.head
      } yield (address, region)

      dbio map { case (address, region) ⇒
        responses.Addresses.build(address, region)
      }
    }

    DBIO.sequence(items).run().futureValue
  }

  val sortColumnName = "name"

  def responseItemsSort(items: IndexedSeq[responses.Addresses.Root]) = items.sortBy(_.name)

  def mf = implicitly[scala.reflect.Manifest[responses.Addresses.Root]]
  // paging and sorting API end

  def validateDeleteResponse(response: HttpResponse) {
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe 'empty
  }

  "GET /v1/customers/:customerId/addresses" - {
    "lists addresses" in new AddressFixture {
      val response = GET(s"v1/customers/${customer.id}/addresses")

      response.status must === (StatusCodes.OK)

      val addresses = response.as[ResponseWithFailuresAndMetadata[Seq[responses.Addresses.Root]]].result

      addresses must have size 1
      addresses.head.name must === (address.name)
    }
  }

  "POST /v1/customers/:customerId/addresses" - {
    "creates an address" in new CustomerFixture {
      val payload = payloads.CreateAddressPayload(name = "Home Office", regionId = 1, address1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      val response = POST(s"v1/customers/${customer.id}/addresses", payload)

      response.status must === (StatusCodes.OK)

      val newAddress = response.as[responses.Addresses.Root]

      newAddress.name must === (payload.name)
      newAddress.isDefault must === (None)
    }

  }

  "POST /v1/customers/:customerId/addresses/:addressId/default" - {
    "sets the isDefaultShippingAddress flag on an address" in new AddressFixture {
      val payload = payloads.ToggleDefaultShippingAddress(isDefault = false)
      val response = POST(s"v1/customers/${customer.id}/addresses/${address.id}/default", payload)

      response.status must === (StatusCodes.NoContent)
    }

    "sets a new shipping address if there's already a default shipping address" in new AddressFixture {
      val another = Addresses.save(address.copy(id = 0, isDefaultShipping = false)).futureValue
      val payload = payloads.ToggleDefaultShippingAddress(isDefault = true)
      val response = POST(s"v1/customers/${customer.id}/addresses/${another.id}/default", payload)

      response.status must === (StatusCodes.NoContent)

      Addresses.findOneById(another.id).futureValue.value.isDefaultShipping mustBe true
      Addresses.findOneById(address.id).futureValue.value.isDefaultShipping mustBe false
    }
  }

  "DELETE /v1/customers/:customerId/addresses/default" - {
    "removes an existing default from a shipping address" in new AddressFixture {
      val response = DELETE(s"v1/customers/${customer.id}/addresses/default")

      validateDeleteResponse(response)

      Addresses.findOneById(address.id).futureValue.value.isDefaultShipping mustBe false
    }

    "attempts to removes default shipping address when none is set" in new CustomerFixture {
      val response = DELETE(s"v1/customers/${customer.id}/addresses/default")

      validateDeleteResponse(response)

      Addresses.findAllByCustomerId(customer.id).length.result.run().futureValue must === (0)
    }
  }

  "PATCH /v1/customers/:customerId/addresses/:addressId" - {
    "can be edited" in new AddressFixture {
      val payload = payloads.CreateAddressPayload(name = "Home Office", regionId = 1, address1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      (payload.name, payload.address1) must !== ((address.name, address.address1))

      val response = PATCH(s"v1/customers/${customer.id}/addresses/${address.id}", payload)

      val updated = response.as[responses.Addresses.Root]
      response.status must === (StatusCodes.OK)

      (updated.name, updated.address1) must === ((payload.name, payload.address1))
    }

  }

  "DELETE /v1/customers/:customerId/addresses/:addressId" - {
    "can be deleted" in new AddressFixture {

      //notice the payload is a default shipping address. Delete should make it not default.
      val payload = payloads.CreateAddressPayload(
        name = "Delete Me", regionId = 1, address1 = "5000 Delete Dr",
        city = "Deattle", zip = "666", isDefault = true)

      val response = POST(s"v1/customers/${customer.id}/addresses", payload)
      response.status must === (StatusCodes.OK)
      val newAddress = response.as[responses.Addresses.Root]

      //now delete
      val deleteResponse = DELETE(s"v1/customers/${customer.id}/addresses/${newAddress.id}")
      validateDeleteResponse(deleteResponse)

      //now get
      val getResponse = GET(s"v1/customers/${customer.id}/addresses/${newAddress.id}")
      getResponse.status must === (StatusCodes.OK)
      val gotAddress = getResponse.as[responses.Addresses.Root]

      //deleted address is not default anymore
      gotAddress.isDefault.value mustBe false

      //deleted address should have a deletedAt timestamp
      gotAddress.deletedAt mustBe defined

      val addressesResponse = GET(s"v1/customers/${customer.id}/addresses")
      addressesResponse.status must === (StatusCodes.OK)

      //If you get all the addresses, our newly deleted one should not show up
      val addresses = addressesResponse.as[ResponseWithFailuresAndMetadata[Seq[responses.Addresses.Root]]].result
      addresses.filter(_.id == newAddress.id) must have length 0
    }

    "fails deleting using wrong address id" in new AddressFixture {
      val wrongAddressId = 47423987

      val response = DELETE(s"v1/customers/${customer.id}/addresses/$wrongAddressId")
      response.status must === (StatusCodes.NotFound)
    }

    "fails deleting using wrong customer id" in new AddressFixture {
      val wrongCustomerId = 44443
      val response = DELETE(s"v1/customers/$wrongCustomerId/addresses/${address.id}")
      response.status must === (StatusCodes.NotFound)
    }
  }

  "GET /v1/customers/:customerId/addresses/display" - {
    "display address" - {
      "succeeds when there is a default shipping address" in new AddressFixture {
        val response = GET(s"v1/customers/${customer.id}/addresses/display")
        response.status must === (StatusCodes.OK)
      }

      "succeeds when there is no default shipping address but a previous order" in new NoDefaultAddressFixture {
        val response = GET(s"v1/customers/${customer.id}/addresses/display")
        response.status must === (StatusCodes.OK)
      }

      "fails when there are no orders or default shipping addresses" in new CustomerFixture {
        val response = GET(s"v1/customers/${customer.id}/addresses/display")
        response.status must === (StatusCodes.BadRequest)
      }
    }
  }

  trait CustomerFixture {
    val customer = Customers.save(Factories.customer).futureValue
  }

  trait AddressFixture extends CustomerFixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id,
      isDefaultShipping = true)).futureValue
  }

  trait ShippingAddressFixture extends AddressFixture {
    (for {
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.copyFromAddress(address, order.id)
    } yield (order, shippingAddress)).futureValue
  }

  trait NoDefaultAddressFixture extends CustomerFixture {
    val (address, order, shippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.copyFromAddress(address, order.id)
    } yield (address, order, shippingAddress)).run().futureValue
  }
}