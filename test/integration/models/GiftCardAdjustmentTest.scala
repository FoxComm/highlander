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
        origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
        adjustment ← GiftCards.adjust(giftCard = gc, debit = 0, credit = -1, capture = false)
      } yield (gc, adjustment)

      val failure = inserts.run().failed.futureValue
      failure.getMessage must include ("""violates check constraint "valid_entry"""")
    }

    "only one of credit or debit can be greater than zero" in new Fixture {
      val inserts = for {
        origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
        adjustment ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 50, capture = false)
      } yield (gc, adjustment)

      val failure = inserts.run().failed.futureValue
      failure.getMessage must include ("""violates check constraint "valid_entry"""")
    }

    "one of credit or debit must be greater than zero" in new Fixture {
      val (_, adjustment) = (for {
        origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, originalBalance = 50))
        adjustment ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 0, capture = true)
      } yield (gc, adjustment)).run().futureValue

      adjustment.id must === (1)
    }

    "updates the GiftCard's currentBalance and availableBalance after insert" in new Fixture {
      val gc = (for {
        origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, originalBalance = 500))
        _ ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 0, capture = true)
        _ ← GiftCards.adjust(giftCard = gc, debit = 25, credit = 0, capture = true)
        _ ← GiftCards.adjust(giftCard = gc, debit = 15, credit = 0, capture = true)
        _ ← GiftCards.adjust(giftCard = gc, debit = 10, credit = 0, capture = true)
        _ ← GiftCards.adjust(giftCard = gc, debit = 100, credit = 0, capture = false)
        _ ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 0, capture = false)
        _ ← GiftCards.adjust(giftCard = gc, debit = 50, credit = 0, capture = false)
        _ ← GiftCards.adjust(giftCard = gc, debit = 200, credit = 0, capture = true)
        gc ← GiftCards.findById(gc.id)
      } yield gc.get).run().futureValue

      gc.availableBalance must === (0)
      gc.currentBalance must === (200)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val admin = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
    } yield admin).run().futureValue
  }
}

