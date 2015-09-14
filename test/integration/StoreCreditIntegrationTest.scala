import akka.http.scaladsl.model.StatusCodes

import models.{Customer, Reasons, Customers, StoreCredit, StoreCredits, StoreAdmins}
import org.scalatest.BeforeAndAfterEach
import services.NotFoundFailure
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class StoreCreditIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "StoreCredits" - {
    "POST /v1/customers/:id/payment-methods/store-credit" - {
      "when successful" - {
        "responds with the new storeCredit" in new Fixture {
          val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = reason.id)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)
          val sc = response.as[responses.StoreCreditResponse.Root]

          response.status must === (StatusCodes.OK)
          sc.status must === (StoreCredit.Active)
        }
      }

      "fails if the customer is not found" in {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = 1)
        val response = POST(s"v1/customers/99/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.errors must === (NotFoundFailure(Customer, 99).description)
      }
    }
  }

  trait Fixture {
    val (admin, customer, reason) = (for {
      admin     ← StoreAdmins.save(authedStoreAdmin)
      customer  ← Customers.save(Factories.customer)
      reason    ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, reason)).run().futureValue
  }
}

