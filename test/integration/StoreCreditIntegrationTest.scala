import akka.http.scaladsl.model.StatusCodes

import models.{Customers, StoreAdmins, StoreCredit, StoreCreditCsrs, StoreCredits}
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

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (admin, customer) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
    } yield (admin, customer)).run().futureValue
  }

  "returns store credits belonging to the customer" in new Fixture {
    val sc = (for {
      origin ← StoreCreditCsrs.save(Factories.storeCreditCsr.copy(adminId = admin.id))
      sc ← StoreCredits.save(Factories.storeCredit.copy(customerId = customer.id, originId = origin.id))
    } yield sc).run().futureValue

    val response = GET(s"v1/users/${customer.id}/payment-methods/store-credits")
    val storeCredit = parse(response.bodyText).extract[Seq[StoreCredit]].head

    response.status must === (StatusCodes.OK)
    storeCredit.customerId must === (customer.id)
  }

  "returns an empty array when the customer has no store credits" in new Fixture {
    val response = GET(s"v1/users/${customer.id}/payment-methods/store-credits")
    val storeCredits = parse(response.bodyText).extract[Seq[StoreCredit]]

    response.status must === (StatusCodes.OK)
    storeCredits mustBe 'empty
  }
}

