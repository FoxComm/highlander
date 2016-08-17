package models

import models.cord.OrderPayments
import models.payment.giftcard._
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class GiftCardAdjustmentIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with TestObjectContext {

  import api._

  "GiftCardAdjustment" - {
    "neither credit nor debit can be negative" in new Fixture {
      val inserts = for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
        gc ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id))
        payment ← * <~ OrderPayments.create(
                     Factories.giftCardPayment.copy(cordRef = cart.refNum,
                                                    paymentMethodId = gc.id,
                                                    amount = Some(gc.availableBalance)))
        adjustment ← * <~ GiftCards.auth(giftCard = gc,
                                         orderPaymentId = Some(payment.id),
                                         debit = 0,
                                         credit = -1)
      } yield (gc, adjustment)

      val failure = inserts.runTxn().futureValue.leftVal
      failure.getMessage must include("""violates check constraint "valid_entry"""")
    }

    "only one of credit or debit can be greater than zero" in new Fixture {
      val inserts = for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
        gc ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment
                       .copy(cordRef = cart.refNum, paymentMethodId = gc.id, amount = Some(50)))
        adjustment ← * <~ GiftCards.auth(giftCard = gc,
                                         orderPaymentId = Some(payment.id),
                                         debit = 50,
                                         credit = 50)
      } yield (gc, adjustment)

      val failure = inserts.runTxn().futureValue.leftVal
      failure.getMessage must include("""violates check constraint "valid_entry"""")
    }

    "one of credit or debit must be greater than zero" in new Fixture {
      val (_, adjustment) = (for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
        gc ← * <~ GiftCards.create(
                Factories.giftCard.copy(originId = origin.id, originalBalance = 50))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment
                       .copy(cordRef = cart.refNum, paymentMethodId = gc.id, amount = Some(50)))
        adjustment ← * <~ GiftCards.capture(giftCard = gc,
                                            orderPaymentId = Some(payment.id),
                                            debit = 50,
                                            credit = 0)
      } yield (gc, adjustment)).gimme

      adjustment.id must === (1)
    }

    "updates the GiftCard's currentBalance and availableBalance before insert" in new Fixture {
      val gc = (for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
        gc ← * <~ GiftCards.create(
                Factories.giftCard.copy(originId = origin.id, originalBalance = 500))
        payment ← * <~ OrderPayments.create(
                     Factories.giftCardPayment.copy(cordRef = cart.refNum,
                                                    paymentMethodId = gc.id,
                                                    amount = Some(gc.availableBalance)))
        _ ← * <~ GiftCards.capture(giftCard = gc,
                                   orderPaymentId = Some(payment.id),
                                   debit = 50,
                                   credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = gc,
                                   orderPaymentId = Some(payment.id),
                                   debit = 25,
                                   credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = gc,
                                   orderPaymentId = Some(payment.id),
                                   debit = 15,
                                   credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = gc,
                                   orderPaymentId = Some(payment.id),
                                   debit = 10,
                                   credit = 0)
        _ ← * <~ GiftCards.auth(giftCard = gc,
                                orderPaymentId = Some(payment.id),
                                debit = 100,
                                credit = 0)
        _ ← * <~ GiftCards.auth(giftCard = gc,
                                orderPaymentId = Some(payment.id),
                                debit = 50,
                                credit = 0)
        _ ← * <~ GiftCards.auth(giftCard = gc,
                                orderPaymentId = Some(payment.id),
                                debit = 50,
                                credit = 0)
        _ ← * <~ GiftCards.capture(giftCard = gc,
                                   orderPaymentId = Some(payment.id),
                                   debit = 200,
                                   credit = 0)
        gc ← * <~ GiftCards.findOneById(gc.id)
      } yield gc.value).gimme

      gc.availableBalance must === (0)
      gc.currentBalance must === (200)
    }

    "a Postgres trigger updates the adjustment's availableBalance before insert" in new Fixture {
      val (adj, gc) = (for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
        gc ← * <~ GiftCards.create(
                Factories.giftCard.copy(originId = origin.id, originalBalance = 500))
        payment ← * <~ OrderPayments.create(
                     Factories.giftCardPayment.copy(cordRef = cart.refNum,
                                                    paymentMethodId = gc.id,
                                                    amount = Some(gc.availableBalance)))
        adj ← * <~ GiftCards.capture(giftCard = gc,
                                     orderPaymentId = Some(payment.id),
                                     debit = 50,
                                     credit = 0)
        adj ← * <~ GiftCardAdjustments.refresh(adj)
        gc  ← * <~ GiftCards.refresh(gc)
      } yield (adj, gc)).value.gimme

      gc.availableBalance must === (450)
      gc.currentBalance must === (450)
      adj.availableBalance must === (gc.availableBalance)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      val (gc, payment) = (for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
        gc ← * <~ GiftCards.create(
                Factories.giftCard.copy(originId = origin.id, originalBalance = 500))
        payment ← * <~ OrderPayments.create(
                     Factories.giftCardPayment.copy(cordRef = cart.refNum,
                                                    paymentMethodId = gc.id,
                                                    amount = Some(gc.availableBalance)))
      } yield (gc, payment)).gimme

      val debits = List(50, 25, 15, 10)
      val adjustments = DbResultT
        .sequence(debits.map { amount ⇒
          GiftCards
            .capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = amount, credit = 0)
        })
        .gimme

      DBIO
        .sequence(adjustments.map { adj ⇒
          GiftCardAdjustments.cancel(adj.id)
        })
        .gimme

      val finalGc = GiftCards.findOneById(gc.id).gimme.value
      (finalGc.originalBalance, finalGc.availableBalance, finalGc.currentBalance) must === (
          (500, 500, 500))
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed {
    val reason = Reasons.create(Factories.reason.copy(storeAdminId = storeAdmin.id)).gimme
  }
}
