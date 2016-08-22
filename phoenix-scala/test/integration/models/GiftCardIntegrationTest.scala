package models

import models.cord.OrderPayments
import models.payment.PaymentMethod
import models.payment.giftcard.{GiftCardAdjustments, GiftCardManual, GiftCardManuals, GiftCards}
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class GiftCardIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with TestObjectContext {

  "GiftCardTest" - {
    "generates a unique alpha-numeric code of size 16 upon insert" in new Fixture {
      giftCard.code must have size (16)
    }

    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      giftCard.originalBalance must === (50)
      giftCard.currentBalance must === (50)
      giftCard.availableBalance must === (50)
    }

    "updates availableBalance if auth adjustment is created + cancel handling" in new Fixture {
      val adjustment = GiftCards.capture(giftCard, Some(payment.id), 10).gimme

      val updatedGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      updatedGiftCard.availableBalance must === (giftCard.availableBalance - 10)

      GiftCardAdjustments.cancel(adjustment.id).run().futureValue
      val canceledGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      canceledGiftCard.availableBalance must === (giftCard.availableBalance)
    }

    "updates availableBalance and currentBalance if capture adjustment is created + cancel handling" in new Fixture {
      val adjustment = GiftCards.capture(giftCard, Some(payment.id), 0, 10).gimme

      val updatedGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      updatedGiftCard.availableBalance must === (giftCard.availableBalance + 10)
      updatedGiftCard.currentBalance must === (giftCard.currentBalance + 10)

      GiftCardAdjustments.cancel(adjustment.id).run().futureValue
      val canceledGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      canceledGiftCard.availableBalance must === (giftCard.availableBalance)
      canceledGiftCard.currentBalance must === (giftCard.currentBalance)
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed {
    val (origin, giftCard, payment) = (for {
      reason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = storeAdmin.id))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
      gc ← * <~ GiftCards.create(
              Factories.giftCard.copy(originalBalance = 50, originId = origin.id))
      giftCard ← * <~ GiftCards.findOneById(gc.id)
      payment ← * <~ OrderPayments.create(
                   Factories.giftCardPayment.copy(cordRef = cart.refNum,
                                                  paymentMethodId = gc.id,
                                                  paymentMethodType = PaymentMethod.GiftCard,
                                                  amount = Some(gc.availableBalance)))
    } yield (origin, giftCard.value, payment)).gimme
  }
}
