import akka.http.scaladsl.model.StatusCodes

import models.{OrderShippingAddresses$, Addresses, Customers}
import util.IntegrationTestBase
import utils.Seeds.Factories
import services.{CustomerHasDefaultShippingAddress, Failure}

class AddressesIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "Addresses" - {
    "lists addresses" in new AddressFixture {
      val response = GET(s"v1/users/${customer.id}/addresses")

      response.status must === (StatusCodes.OK)

      val addresses = parse(response.bodyText).extract[Seq[responses.Addresses.Root]]

      addresses must have size (1)
      addresses.head.name must === (address.name)
    }

    "creates an address" in new CustomerFixture {
      val payload = payloads.CreateAddressPayload(name = "Home Office", stateId = 1, street1 = "3000 Coolio Dr",
        city = "Seattle", zip = "55555")
      val response = POST(s"v1/users/${customer.id}/addresses", payload)

      response.status must === (StatusCodes.OK)

      val newAddress = parse(response.bodyText).extract[responses.Addresses.Root]

      newAddress.name must === (payload.name)
      newAddress.isDefault must === (None)
    }
  }

  "ShippingAddresses" - {
    "list shipping addresses" in new ShippingAddressFixture {
      val response = GET(s"v1/users/${customer.id}/shipping-addresses")

      response.status must === (StatusCodes.OK)

      val addresses = parse(response.bodyText).extract[Seq[responses.Addresses.Root]]

      addresses must have size (1)
      addresses.head.name must === (address.name)
      addresses.head.isDefault must === (Some(true))
    }

    "sets the isDefault flag on a shipping address" in new ShippingAddressFixture {
      val payload = payloads.ToggleDefaultShippingAddress(isDefault = false)
      val response = POST(s"v1/users/${customer.id}/shipping-addresses/${shippingAddress.id}/default", payload)

      response.status must === (StatusCodes.OK)
      response.bodyText mustBe 'empty
    }

    "errors if there's already a default shipping address" in new ShippingAddressFixture {
      val (_, another) = OrderShippingAddresses.createFromAddress(address.copy(id = 0)).run().futureValue
      val payload = payloads.ToggleDefaultShippingAddress(isDefault = true)
      val response = POST(s"v1/users/${customer.id}/shipping-addresses/${another.id}/default", payload)

      response.status must === (StatusCodes.BadRequest)

      val errors = parse(response.bodyText).extract[Map[String, Seq[String]]]
      errors must === (Map("errors" -> CustomerHasDefaultShippingAddress.description))
    }
  }

  trait CustomerFixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }

  trait AddressFixture extends CustomerFixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id)).run().futureValue
  }

  trait ShippingAddressFixture extends AddressFixture {
    val (_, shippingAddress) = OrderShippingAddresses.createFromAddress(address, isDefault = true).run().futureValue
  }
}

