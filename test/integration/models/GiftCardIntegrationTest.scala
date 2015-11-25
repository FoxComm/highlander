package models

import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Seeds.Factories
import utils.Slick.implicits._

class GiftCardIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "GiftCardTest" - {
    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      giftCard.originalBalance must === (50)
      giftCard.currentBalance must === (50)
      giftCard.availableBalance must === (50)
    }

    "updates availableBalance if auth adjustment is created + cancel handling" in new Fixture {
      val adjustment = GiftCards.capture(giftCard, Some(payment.id), 10).run().futureValue.rightVal

      val updatedGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      updatedGiftCard.availableBalance must === (giftCard.availableBalance - 10)

      GiftCardAdjustments.cancel(adjustment.id).run().futureValue
      val canceledGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      canceledGiftCard.availableBalance must === (giftCard.availableBalance)
    }

    "updates availableBalance and currentBalance if capture adjustment is created + cancel handling" in new Fixture {
      val adjustment = GiftCards.capture(giftCard, Some(payment.id), 0, 10).run().futureValue.rightVal

      val updatedGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      updatedGiftCard.availableBalance must === (giftCard.availableBalance + 10)
      updatedGiftCard.currentBalance must === (giftCard.currentBalance + 10)

      GiftCardAdjustments.cancel(adjustment.id).run().futureValue
      val canceledGiftCard = GiftCards.findOneById(giftCard.id).run().futureValue.value
      canceledGiftCard.availableBalance must === (giftCard.availableBalance)
      canceledGiftCard.currentBalance must === (giftCard.currentBalance)
    }
  }

  trait Fixture {
    val (origin, giftCard, payment) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      origin   ← * <~ GiftCardManuals.create(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      gc       ← * <~ GiftCards.create(Factories.giftCard.copy(originalBalance = 50, originId = origin.id))
      giftCard ← * <~ GiftCards.findOneById(gc.id).toXor
      payment  ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id, paymentMethodId = gc.id,
        paymentMethodType = PaymentMethod.GiftCard))
    } yield (origin, giftCard.value, payment)).runT().futureValue.rightVal
  }
}

