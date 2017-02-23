package models

import models.payment.giftcard._
import testutils._
import testutils.fixtures.BakedFixtures
import cats.implicits._
import utils.db._

class GiftCardAdjustmentIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with TestObjectContext {

  import api._

  "GiftCardAdjustment" - {
    "neither credit nor debit can be negative" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance

      val failure = GiftCards
        .auth(giftCard = giftCard, orderPaymentId = orderPayments.head.id, debit = -1)
        .gimmeTxnFailures
      failure.getMessage must include("""violates check constraint "valid_entry"""")
    }

    "only one of credit or debit can be greater than zero" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance
      val failure = GiftCards
        .adjust(giftCard = giftCard,
                orderPaymentId = orderPayments.head.id.some,
                debit = 50,
                credit = 50)
        .gimmeTxnFailures
      failure.getMessage must include("""violates check constraint "valid_entry"""")
    }

    "one of credit or debit must be greater than zero" in new Fixture {
      override def gcPaymentAmount = 50

      val adjustment = (for {
        auth ← * <~ GiftCards.auth(giftCard = giftCard,
                                   orderPaymentId = orderPayments.head.id,
                                   debit = 50)
        adjustment ← * <~ GiftCards.capture(giftCard = giftCard,
                                            orderPaymentId = orderPayments.head.id,
                                            debit = 50)
      } yield adjustment).gimme

      adjustment.id must === (1)
    }

    "updates the GiftCard's currentBalance and availableBalance before insert" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance

      val pmtId = orderPayments.head.id

      val updated = (for {
        _        ← * <~ GiftCards.auth(giftCard = giftCard, orderPaymentId = pmtId, debit = 100)
        _        ← * <~ GiftCards.auth(giftCard = giftCard, orderPaymentId = pmtId, debit = 50)
        _        ← * <~ GiftCards.auth(giftCard = giftCard, orderPaymentId = pmtId, debit = 50)
        _        ← * <~ GiftCards.capture(giftCard = giftCard, orderPaymentId = pmtId, debit = 50)
        _        ← * <~ GiftCards.capture(giftCard = giftCard, orderPaymentId = pmtId, debit = 25)
        _        ← * <~ GiftCards.capture(giftCard = giftCard, orderPaymentId = pmtId, debit = 15)
        giftCard ← * <~ GiftCards.refresh(giftCard)
      } yield giftCard).gimme

      updated.availableBalance must === (500 - 50 - 25 - 15)
      updated.currentBalance must === (500 - 50 - 25 - 15)
    }

    "a Postgres trigger updates the adjustment's availableBalance before insert" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance

      GiftCards.auth(giftCard = giftCard, orderPaymentId = orderPayments.head.id, debit = 50).gimme
      val adj = GiftCards
        .capture(giftCard = giftCard, orderPaymentId = orderPayments.head.id, debit = 50)
        .gimme
      val updated = GiftCards.refresh(giftCard).gimme

      updated.availableBalance must === (450)
      updated.currentBalance must === (450)
      adj.availableBalance must === (updated.availableBalance)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance

      val debits = List(50, 25, 15, 10)
      def auth(amount: Int) =
        GiftCards.auth(giftCard = giftCard, orderPaymentId = orderPayments.head.id, debit = amount)
      val adjustments = DbResultT.sequenceJoiningFailures((1 to 4).toList.map(auth)).gimme

      adjustments.map { adj ⇒
        GiftCardAdjustments.cancel(adj.id).gimme
      }

      val finalGc = GiftCards.refresh(giftCard).gimme
      (finalGc.originalBalance, finalGc.availableBalance, finalGc.currentBalance) must === (
          (500, 500, 500))
    }
  }

  trait Fixture
      extends Reason_Baked
      with EmptyCustomerCart_Baked
      with GiftCard_Raw
      with CartWithGiftCardPayment_Raw {
    override def giftCardBalance = 500
  }
}
