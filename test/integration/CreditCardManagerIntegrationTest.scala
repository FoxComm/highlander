import akka.http.scaladsl.model.StatusCodes

import models.{Orders, Customer, CreditCards, CreditCard, Customers, Addresses, StoreAdmins, OrderPayments}
import models.OrderPayments.scope._
import org.joda.time.DateTime
import services.{CannotUseInactiveCreditCard, CustomerManager, NotFoundFailure}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.RunOnDbIO

class CreditCardManagerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._
  import slick.driver.PostgresDriver.api._

  "CreditCardManagerTest" - {
    "when creating a credit card" - {
      val tomorrow = DateTime.now().plusDays(1)
      val payload = payloads.CreateCreditCard(holderName = "yax", number = "4444-4444-4444-4444",
        cvv = "123", expYear = tomorrow.getYear, expMonth = tomorrow.getMonthOfYear)

      "Adds a credit card with a billing address that does not exist in the address book" in new Fixture {
        val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)

        response.status must ===(StatusCodes.OK)
        val (cc :: Nil) = CreditCards.findInWalletByCustomerId(customer.id).result.run().futureValue.toList
      }
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }
}

