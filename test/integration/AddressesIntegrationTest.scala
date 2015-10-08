import akka.http.scaladsl.model.StatusCodes

import models.{Orders, OrderShippingAddresses, Addresses, Customers}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import services.{CustomerHasDefaultShippingAddress, Failure}
import util.SlickSupport.implicits._
import akka.http.scaladsl.model.HttpResponse

class AddressesIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {


  import concurrent.ExecutionContext.Implicits.global
  import api._
  import Extensions._
  import org.json4s.jackson.JsonMethods._

  def validateDeleteResponse(response: HttpResponse) { 
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe 'empty
  }

  "Addresses" - {
    "lists addresses" in new AddressFixture {
      val response = GET(s"v1/customers/${customer.id}/addresses")

      response.status must === (StatusCodes.OK)

      val addresses = parse(response.bodyText).extract[Seq[responses.Addresses.Root]]

      addresses must have size (1)
      addresses.head.name must === (address.name)
    }

    "creates an address" in new CustomerFixture {
      val payload = payloads.CreateAddressPayload(name = "Home Office", regionId = 1, address1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      val response = POST(s"v1/customers/${customer.id}/addresses", payload)

      response.status must === (StatusCodes.OK)

      val newAddress = parse(response.bodyText).extract[responses.Addresses.Root]

      newAddress.name must === (payload.name)
      newAddress.isDefault must === (None)
    }

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

      Addresses.findById(another.id).futureValue.value.isDefaultShipping mustBe true
      Addresses.findById(address.id).futureValue.value.isDefaultShipping mustBe false
    }

    "removes an existing default from a shipping address" in new AddressFixture {
      val response = DELETE(s"v1/customers/${customer.id}/addresses/default")

      validateDeleteResponse(response)

      Addresses.findById(address.id).futureValue.value.isDefaultShipping mustBe false
    }

    "attempts to removes default shipping address when none is set" in new CustomerFixture {
      val response = DELETE(s"v1/customers/${customer.id}/addresses/default")

      validateDeleteResponse(response)

      Addresses._findAllByCustomerId(customer.id).length.result.run().futureValue must === (0)
    }

    "can be edited" in new AddressFixture {
      val payload = payloads.CreateAddressPayload(name = "Home Office", regionId = 1, address1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      (payload.name, payload.address1) must !== ((address.name, address.address1))

      val response = PATCH(s"v1/customers/${customer.id}/addresses/${address.id}", payload)

      val updated = parse(response.bodyText).extract[responses.Addresses.Root]
      response.status must === (StatusCodes.OK)

      (updated.name, updated.address1) must === ((payload.name, payload.address1))
    }

    "can be deleted" in new AddressFixture { 

      //notice the payload is a default shipping address. Delete should make it not default.
      val payload = payloads.CreateAddressPayload(name = "Delete Me", regionId = 1, address1 = "5000 Delete Dr", city = "Deattle", zip = "666", isDefault = true)

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
      gotAddress.isDefault match {
        case None ⇒  info("Isn't strange that a boolean is optional")
        case Some(isDefault) ⇒  isDefault must be (false)
      }

      //deleted address should have a deletedAt timestamp
      gotAddress.deletedAt match { 
        case None ⇒  fail("FullOrder should have a shipping address")
        case Some(time) ⇒  info(s"Deleted on ${time}")
      }

      val addressesResponse = GET(s"v1/customers/${customer.id}/addresses")
      addressesResponse.status must === (StatusCodes.OK)

      //If you get all the addresses, our newly deleted one should not show up
      val addresses = parse(addressesResponse.bodyText).extract[Seq[responses.Addresses.Root]]
      addresses.filter(_.id == newAddress.id) must have length (0)
    }

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
        response.status must === (StatusCodes.NotFound)
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

