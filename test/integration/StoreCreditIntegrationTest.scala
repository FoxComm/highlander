import akka.http.scaladsl.model.StatusCodes

import models.{Reasons, Customers, StoreAdmins, StoreCredit, StoreCreditManuals, StoreCredits}
import org.scalatest.BeforeAndAfterEach
import util.IntegrationTestBase
import utils.Seeds.Factories

class StoreCreditIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "admin API" - {
    "returns store credits belonging to the customer" in new Fixture {
      (for {
        origin ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id))
        sc ← StoreCredits.save(Factories.storeCredit.copy(customerId = customer.id, originId = origin.id))
      } yield sc).run().futureValue

      val response = GET(s"v1/users/${customer.id}/payment-methods/store-credits")
      val storeCredit = parse(response.bodyText).extract[Seq[StoreCredit]].head

      response.status must ===(StatusCodes.OK)
      storeCredit.customerId must ===(customer.id)
    }

    "returns an empty array when the customer has no store credits" in new Fixture {
      val response = GET(s"v1/users/${customer.id}/payment-methods/store-credits")
      val storeCredits = parse(response.bodyText).extract[Seq[StoreCredit]]

      response.status must ===(StatusCodes.OK)
      storeCredits mustBe 'empty
    }

    "finds a store credit by id" in new Fixture {
      val sc = (for {
        origin ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id))
        sc ← StoreCredits.save(Factories.storeCredit.copy(customerId = customer.id, originId = origin.id))
      } yield sc).run().futureValue

      val response = GET(s"v1/users/${customer.id}/payment-methods/store-credits/${sc.id}")
      val storeCredit = parse(response.bodyText).extract[StoreCredit]

      response.status must ===(StatusCodes.OK)
      storeCredit.customerId must ===(customer.id)

      val notFoundResponse = GET(s"v1/users/${customer.id}/payment-methods/store-credits/99")
      notFoundResponse.status must ===(StatusCodes.NotFound)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (admin, customer, reason) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, reason)).run().futureValue
  }

}

