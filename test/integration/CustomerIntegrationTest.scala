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
    "shows a customer" in new Fixture {
      val response = GET(s"v1/users/${customer.id}")

      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Customer] must === (customer)
    }

    "disables a customer account" in new Fixture {
      val response = PATCH(s"v1/users/${customer.id}/disable")

      response.status must === (StatusCodes.OK)
      val changedCustomer = parse(response.bodyText).extract[Customer]

      customer.disabled must === (false)
      changedCustomer.disabled must === (true)
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).run().futureValue
    val admin = StoreAdmins.save(authedStoreAdmin).run().futureValue
  }
}

