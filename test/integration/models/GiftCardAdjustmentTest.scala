package models

import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils.Seeds.Factories

class GiftCardAdjustmentTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "GiftCardAdjustment" - {
    "neither credit nor debit can be negative" in new Fixture {
      val inserts = for {
        origin ← GiftCardCsrs.save(Factories.giftCardCsr.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
        adjustment ← GiftCards.adjust(giftCard = gc, debit = 0, credit = -1, capture = false)
      } yield (gc, adjustment)

      val failure = inserts.run().failed.futureValue
      failure.getMessage must include ("""violates check constraint "valid_entry"""")
    }

    "only one of credit or debit can be greater than zero" in new Fixture {
      val inserts = for {
        origin ← GiftCardCsrs.save(Factories.giftCardCsr.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
        adjustment ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 50, capture = false)
      } yield (gc, adjustment)

      val failure = inserts.run().failed.futureValue
      failure.getMessage must include ("""violates check constraint "valid_entry"""")
    }

    "one of credit or debit must be greater than zero" in new Fixture {
      val (_, adjustment) = (for {
        origin ← GiftCardCsrs.save(Factories.giftCardCsr.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, originalBalance = 50))
        adjustment ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 0, capture = true)
      } yield (gc, adjustment)).run().futureValue

      adjustment.id === 1
    }

    "updates the GiftCard's currentBalance after insert" in new Fixture {
      val gc = (for {
        origin ← GiftCardCsrs.save(Factories.giftCardCsr.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, originalBalance = 100))
        _ ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 0, capture = true)
        _ ← GiftCards.adjust(giftCard = gc, debit = 25, credit = 0, capture = true)
        _ ← GiftCards.adjust(giftCard = gc, debit = 15, credit = 0, capture = true)
      } yield gc).run().futureValue

      GiftCards.findById(gc.id).run().futureValue.get.currentBalance === (15)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (admin, customer) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
    } yield (admin, customer)).run().futureValue
  }
}

