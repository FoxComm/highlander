package models

import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class GiftCardIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "GiftCardTest" - {
    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      giftCard.originalBalance must === (50)
      giftCard.currentBalance must === (50)
      giftCard.availableBalance must === (50)
    }

    "updates availableBalance if auth adjustment is created + cancel handling" in new Fixture {
      val adjustment = GiftCards.capture(giftCard, Some(payment.id), 10).run().futureValue

      val updatedGiftCard = GiftCards.findById(giftCard.id).run().futureValue.value
      updatedGiftCard.availableBalance must === (giftCard.availableBalance - 10)

      GiftCardAdjustments.cancel(adjustment.id).run().futureValue
      val canceledGiftCard = GiftCards.findById(giftCard.id).run().futureValue.value
      canceledGiftCard.availableBalance must === (giftCard.availableBalance)
    }

    "updates availableBalance and currentBalance if capture adjustment is created + cancel handling" in new Fixture {
      val adjustment = GiftCards.capture(giftCard, Some(payment.id), 0, 10).run().futureValue

      val updatedGiftCard = GiftCards.findById(giftCard.id).run().futureValue.value
      updatedGiftCard.availableBalance must === (giftCard.availableBalance + 10)
      updatedGiftCard.currentBalance must === (giftCard.currentBalance + 10)

      GiftCardAdjustments.cancel(adjustment.id).run().futureValue
      val canceledGiftCard = GiftCards.findById(giftCard.id).run().futureValue.value
      canceledGiftCard.availableBalance must === (giftCard.availableBalance)
      canceledGiftCard.currentBalance must === (giftCard.currentBalance)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (origin, giftCard, payment) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      gc ← GiftCards.save(Factories.giftCard.copy(originalBalance = 50, originId = origin.id))
      giftCard ← GiftCards.findById(gc.id)
      payment ← OrderPayments.save(Factories.giftCardPayment.copy(orderId = order.id, paymentMethodId = gc.id,
        paymentMethodType = PaymentMethod.GiftCard))
    } yield (origin, giftCard.value, payment)).run().futureValue
  }
}

