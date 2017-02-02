package models

import models.payment.giftcard._
import testutils._
import testutils.fixtures.BakedFixtures

class GiftCardIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with TestObjectContext {

  "GiftCardTest" - {
    "generates a unique alpha-numeric code of size 16 upon insert" in new SimpleFixture {
      giftCard.code must have size 16
    }

    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new SimpleFixture {
      giftCard.originalBalance must === (50)
      giftCard.currentBalance must === (50)
      giftCard.availableBalance must === (50)
    }

    "updates availableBalance if auth adjustment is created + cancel handling" in new Fixture {
      val auth       = GiftCards.auth(giftCard, orderPayments.head.id, 10).gimme
      val adjustment = GiftCards.capture(giftCard, orderPayments.head.id, 10).gimme

      val updatedGiftCard = GiftCards.refresh(giftCard).gimme
      updatedGiftCard.availableBalance must === (giftCard.originalBalance - 10)

      GiftCardAdjustments.cancel(adjustment.id).gimme
      val canceledGiftCard = GiftCards.refresh(giftCard).gimme
      canceledGiftCard.availableBalance must === (giftCard.originalBalance)
    }
  }

  trait SimpleFixture extends Reason_Baked with GiftCard_Raw {
    override def giftCardBalance = 50
  }

  trait Fixture
      extends SimpleFixture
      with EmptyCustomerCart_Baked
      with CartWithGiftCardOnlyPayment_Raw
}
