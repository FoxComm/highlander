import akka.http.scaladsl.model.StatusCodes

import models.{ShippingAddresses, Addresses, Customers}
import util.IntegrationTestBase
import utils.Seeds.Factories

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
  }

  trait CustomerFixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }

  trait AddressFixture extends CustomerFixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id)).run().futureValue
  }

  trait ShippingAddressFixture extends AddressFixture {
    val shippingAddress = ShippingAddresses.createFromAddress(address, isDefault = true).run().futureValue
  }
}

