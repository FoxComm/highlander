import akka.http.scaladsl.model.StatusCodes

import models.{Addresses, Customers}
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

    trait AddressFixture {
      val (customer, address) = (for {
        customer ← Customers.save(Factories.customer)
        address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      } yield (customer, address)).run().futureValue
    }
  }
}

