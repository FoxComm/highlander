package models

import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class StoreCreditTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "StoreCreditTest" - {
    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      storeCredit.originalBalance must === (50)
      storeCredit.currentBalance must === (50)
      storeCredit.availableBalance must === (50)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (customer, origin, storeCredit) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id))
      sc ← StoreCredits.save(Factories.storeCredit.copy(customerId = customer.id, originId = origin.id))
      storeCredit ← StoreCredits.findById(sc.id)
    } yield (customer, origin, storeCredit.get)).run().futureValue
  }
}


