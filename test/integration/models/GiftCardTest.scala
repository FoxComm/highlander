package models

import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils.Seeds.Factories

class GiftCardTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "GiftCardTest" - {
    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      giftCard.originalBalance must === (50)
      giftCard.currentBalance must === (50)
      giftCard.availableBalance must === (50)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (customer, origin, giftCard) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
      origin ← GiftCardCsrs.save(Factories.giftCardCsr.copy(adminId = admin.id))
      gc ← GiftCards.save(Factories.giftCard.copy(originalBalance = 50, customerId = Some(customer.id),
        originId = origin.id))
      giftCard ← GiftCards.findById(gc.id)
    } yield (customer, origin, giftCard.get)).run().futureValue
  }
}

