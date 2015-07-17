import akka.http.scaladsl.model.StatusCodes
import models._
import org.joda.time.DateTime
import org.scalatest.time.{Milliseconds, Seconds, Span}
import payloads.{CreateAddressPayload, CreditCardPayload}
import responses.{AdminNotes, FullOrder}
import services.NoteManager
import util.{IntegrationTestBase, StripeSupport}
import utils.Seeds.Factories

class CustomerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import org.json4s.jackson.JsonMethods._
  import Extensions._

  "admin APIs" - {
    "shows a customer" in new CustomerFixture {
      val response = GET(s"v1/users/${customer.id}")

      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Customer] must === (customer)
    }

    "enables a customer account" in new CustomerFixture {
      val response = GET(s"v1/users/${customer.id}/enable")

      response.status must === (StatusCodes.OK)
      val respCustomer = parse(response.bodyText).extract[Customer]

      respCustomer.disabled must === (true)
      respCustomer.disabled must !== (customer.disabled)
    }
  }

  trait CustomerFixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }
}

