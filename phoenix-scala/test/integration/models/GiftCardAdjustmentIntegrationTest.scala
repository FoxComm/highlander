package models

import models.payment.giftcard._
import testutils._
import testutils.fixtures.BakedFixtures
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
        .auth(giftCard = giftCard,
              orderPaymentId = Some(orderPayments.head.id),
              debit = 0,
              credit = -1)
        .runTxn()
        .futureValue
        .leftVal
      failure.getMessage must include("""violates check constraint "valid_entry"""")
    }

    "only one of credit or debit can be greater than zero" in new Fixture {
      override def gcPaymentAmount = 50

      val failure = GiftCards
        .auth(giftCard = giftCard,
              orderPaymentId = Some(orderPayments.head.id),
              debit = 50,
              credit = 50)
        .runTxn()
        .futureValue
        .leftVal
      failure.getMessage must include("""violates check constraint "valid_entry"""")
    }

    "one of credit or debit must be greater than zero" in new Fixture {
      override def gcPaymentAmount = 50

      val adjustment = (for {
        adjustment ← * <~ GiftCards.capture(giftCard = giftCard,
                                            orderPaymentId = Some(orderPayments.head.id),
                                            debit = 50,
                                            credit = 0)
      } yield adjustment).gimme

      adjustment.id must === (1)
    }

    "updates the GiftCard's currentBalance and availableBalance before insert" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance

      val pmtId = orderPayments.head.id

      val updated = (for {
        _ ← * <~ GiftCards.capture(giftCard = giftCard,
                                   orderPaymentId = Some(pmtId),
                                   debit = 50,
                                   credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = giftCard,
                                   orderPaymentId = Some(pmtId),
                                   debit = 25,
                                   credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = giftCard,
                                   orderPaymentId = Some(pmtId),
                                   debit = 15,
                                   credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = giftCard,
                                   orderPaymentId = Some(pmtId),
                                   debit = 10,
                                   credit = 0)
        _ ← * <~ GiftCards.auth(giftCard = giftCard,
                                orderPaymentId = Some(pmtId),
                                debit = 100,
                                credit = 0)
        _ ← * <~ GiftCards.auth(giftCard = giftCard,
                                orderPaymentId = Some(pmtId),
                                debit = 50,
                                credit = 0)
        _ ← * <~ GiftCards.auth(giftCard = giftCard,
                                orderPaymentId = Some(pmtId),
                                debit = 50,
                                credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = giftCard,
                                   orderPaymentId = Some(pmtId),
                                   debit = 200,
                                   credit = 0)
        giftCard ← * <~ GiftCards.refresh(giftCard)
      } yield giftCard).gimme

      updated.availableBalance must === (0)
      updated.currentBalance must === (200)
    }

    "a Postgres trigger updates the adjustment's availableBalance before insert" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance

      val (adj, updated) = (for {
        adj ← * <~ GiftCards.capture(giftCard = giftCard,
                                     orderPaymentId = Some(orderPayments.head.id),
                                     debit = 50,
                                     credit = 0)
        giftCard ← * <~ GiftCards.refresh(giftCard)
      } yield (adj, giftCard)).value.gimme

      updated.availableBalance must === (450)
      updated.currentBalance must === (450)
      adj.availableBalance must === (updated.availableBalance)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      override def gcPaymentAmount = giftCard.availableBalance

      val debits = List(50, 25, 15, 10)
      def capture(amount: Int) =
        GiftCards.capture(giftCard = giftCard,
                          orderPaymentId = Some(orderPayments.head.id),
                          debit = amount,
                          credit = 0)
      val adjustments = DbResultT.sequence((1 to 4).map(capture)).gimme

      DBIO
        .sequence(adjustments.map { adj ⇒
          GiftCardAdjustments.cancel(adj.id)
        })
        .gimme

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
