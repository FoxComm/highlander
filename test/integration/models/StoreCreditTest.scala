package models

import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils.Seeds.Factories

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
      origin ← StoreCreditCsrs.save(Factories.storeCreditCsr.copy(adminId = admin.id))
      sc ← StoreCredits.save(Factories.storeCredit.copy(customerId = customer.id, originId = origin.id))
      storeCredit ← StoreCredits.findById(sc.id)
    } yield (customer, origin, storeCredit.get)).run().futureValue
  }
}


