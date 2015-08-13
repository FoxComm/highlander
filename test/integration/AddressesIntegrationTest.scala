import akka.http.scaladsl.model.StatusCodes

import models.{Orders, OrderShippingAddresses, Addresses, Customers}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._
import services.{CustomerHasDefaultShippingAddress, Failure}

class AddressesIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import api._
  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "Addresses" - {
    "lists addresses" in new AddressFixture {
      val response = GET(s"v1/customers/${customer.id}/addresses")

      response.status must === (StatusCodes.OK)

      val addresses = parse(response.bodyText).extract[Seq[responses.Addresses.Root]]

      addresses must have size (1)
      addresses.head.name must === (address.name)
    }

    "creates an address" in new CustomerFixture {
      val payload = payloads.CreateAddressPayload(name = "Home Office", stateId = 1, street1 = "3000 Coolio Dr",
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

      response.status must === (StatusCodes.OK)
      response.bodyText mustBe 'empty
    }

    "sets a new shipping address if there's already a default shipping address" in new AddressFixture {
      val another = Addresses.save(address.copy(id = 0, isDefaultShipping = false)).run().futureValue
      val payload = payloads.ToggleDefaultShippingAddress(isDefault = true)
      val response = POST(s"v1/customers/${customer.id}/addresses/${another.id}/default", payload)

      response.status must === (StatusCodes.OK)
      response.bodyText mustBe 'empty

      Addresses.findById(another.id).run().futureValue.get.isDefaultShipping mustBe true
      Addresses.findById(address.id).run().futureValue.get.isDefaultShipping mustBe false
    }

    "removes an existing default from a shipping address" in new AddressFixture {
      val response = DELETE(s"v1/customers/${customer.id}/addresses/default")

      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe 'empty

      Addresses.findById(address.id).run().futureValue.get.isDefaultShipping mustBe false
    }

    "attempts to removes default shipping address when none is set" in new CustomerFixture {
      val response = DELETE(s"v1/customers/${customer.id}/addresses/default")

      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe 'empty

      Addresses._findAllByCustomerId(customer.id).length.result.run().futureValue must === (0)
    }

    "can be edited" in new AddressFixture {
      val payload = payloads.CreateAddressPayload(name = "Home Office", stateId = 1, street1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      (payload.name, payload.street1) must !== ((address.name, address.street1))

      val response = PATCH(s"v1/customers/${customer.id}/addresses/${address.id}", payload)

      val updated = parse(response.bodyText).extract[responses.Addresses.Root]
      response.status must === (StatusCodes.OK)

      (updated.name, updated.street1) must === ((payload.name, payload.street1))
    }
  }

  trait CustomerFixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }

  trait AddressFixture extends CustomerFixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id,
      isDefaultShipping = true)).run().futureValue
  }

  trait ShippingAddressFixture extends AddressFixture {
    (for {
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.copyFromAddress(address, order.id)
    } yield (order, shippingAddress)).run().futureValue
  }
}

