import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import java.time.Instant

import models.customer.{Customers, Customer}
import models.location.{Address, Addresses, Regions}
import models.order.{OrderShippingAddresses, Orders}
import services.NotFoundFailure404
import util.IntegrationTestBase
import util.SlickSupport.implicits._
import utils.DbResultT
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.RankingSeedsGenerator

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
    currentCustomer = Customers.create(Factories.customer).futureValue.rightVal
  }

  def uriPrefix = s"v1/customers/${currentCustomer.id}/addresses"

  def responseItems = {
    val items = (1 to numOfResults).map { i ⇒
      for {
        address ← * <~ Addresses.create(RankingSeedsGenerator.generateAddress.copy(customerId = currentCustomer.id))
        region  ← * <~ Regions.mustFindById404(address.regionId)
      } yield responses.Addresses.build(address, region)
    }

    DbResultT.sequence(items).runTxn().futureValue.rightVal
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

      val addresses = response.ignoreFailuresAndGiveMe[Seq[responses.Addresses.Root]]

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
      newAddress.isDefault must === (Some(false))
    }

  }

  "POST /v1/customers/:customerId/addresses/:addressId/default" - {
    "sets the isDefaultShippingAddress flag on an address" in new NoDefaultAddressFixture {
      val response = POST(s"v1/customers/${customer.id}/addresses/${address.id}/default")
      response.status must === (StatusCodes.OK)
      Addresses.findOneById(address.id).futureValue.value.isDefaultShipping mustBe true
    }

    "sets a new shipping address if there's already a default shipping address" in new AddressFixture {
      val another = Addresses.create(address.copy(id = 0, isDefaultShipping = false)).futureValue.rightVal
      val response = POST(s"v1/customers/${customer.id}/addresses/${another.id}/default")

      response.status must === (StatusCodes.OK)

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

      val deletedAddress = Addresses.findOneById(newAddress.id).run().futureValue.value
      deletedAddress.isDefaultShipping mustBe false
      deletedAddress.deletedAt mustBe defined
    }

    "deleted address should be visible to StoreAdmin" in new DeletedAddressFixture {
      val response = GET(s"v1/customers/${customer.id}/addresses/${address.id}")
      response.status must === (StatusCodes.OK)
    }

    "deleted address should be invisible to Customer" in new DeletedAddressFixture {
      val response = GET(s"v1/my/addresses/${address.id}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Address, address.id).description)
    }

    "fails deleting using wrong address id" in new AddressFixture {
      val response = DELETE(s"v1/customers/${customer.id}/addresses/65536")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Address, 65536).description)
    }

    "fails deleting using wrong customer id" in new AddressFixture {
      val response = DELETE(s"v1/customers/65536/addresses/${address.id}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 65536).description)
    }
  }

  trait CustomerFixture {
    val customer = Customers.create(Factories.customer).futureValue.rightVal
  }

  trait AddressFixture extends CustomerFixture {
    val address = Addresses.create(Factories.address.copy(customerId = customer.id,
      isDefaultShipping = true)).futureValue.rightVal
  }

  trait DeletedAddressFixture {
    val (customer,address) = (for {
      customer  ← * <~ Customers.create(authedCustomer)
      address   ← * <~ Addresses.create(Factories.address.copy(customerId = authedCustomer.id,
        isDefaultShipping = false, deletedAt = Some(Instant.now)))
    } yield (customer, address)).runTxn().futureValue.rightVal
  }

  trait ShippingAddressFixture extends AddressFixture {
    (for {
      order           ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      shippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address, order.id)
    } yield (order, shippingAddress)).runTxn().futureValue.rightVal
  }

  trait NoDefaultAddressFixture extends CustomerFixture {
    val (address, order, shippingAddress) = (for {
      address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      order   ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      shipAdd ← * <~ OrderShippingAddresses.copyFromAddress(address, order.id)
    } yield (address, order, shipAdd)).runTxn().futureValue.rightVal
  }
}
