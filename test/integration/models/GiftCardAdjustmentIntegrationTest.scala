package models

import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

class GiftCardAdjustmentIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "GiftCardAdjustment" - {
    "neither credit nor debit can be negative" in new Fixture {
      val inserts = for {
        origin     ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
        gc         ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id))
        payment    ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = gc.id, amount = Some(gc.availableBalance)))
        adjustment ← * <~ GiftCards.auth(giftCard = gc, orderPaymentId = Some(payment.id), debit = 0, credit = -1)
      } yield (gc, adjustment)

      val failure = inserts.runT().futureValue.leftVal
      failure.getMessage must include ("""violates check constraint "valid_entry"""")
    }

    "only one of credit or debit can be greater than zero" in new Fixture {
      val inserts = for {
        origin     ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
        gc         ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id))
        payment    ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = gc.id, amount = Some(50)))
        adjustment ← * <~ GiftCards.auth(giftCard = gc, orderPaymentId = Some(payment.id), debit = 50, credit = 50)
      } yield (gc, adjustment)

      val failure = inserts.runT().futureValue.leftVal
      failure.getMessage must include ("""violates check constraint "valid_entry"""")
    }

    "one of credit or debit must be greater than zero" in new Fixture {
      val (_, adjustment) = (for {
        origin     ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
        gc         ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, originalBalance = 50))
        payment    ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = gc.id, amount = Some(50)))
        adjustment ← * <~ GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = 50, credit = 0)
      } yield (gc, adjustment)).runT().futureValue.rightVal

      adjustment.id must === (1)
    }

    "updates the GiftCard's currentBalance and availableBalance before insert" in new Fixture {
      val gc = (for {
        origin  ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
        gc      ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, originalBalance = 500))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = gc.id, amount = Some(gc.availableBalance)))
        _       ← * <~ GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = 50, credit = 0)
        _       ← * <~ GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = 25, credit = 0)
        _       ← * <~ GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = 15, credit = 0)
        _       ← * <~ GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = 10, credit = 0)
        _       ← * <~ GiftCards.auth(giftCard = gc, orderPaymentId = Some(payment.id), debit = 100, credit = 0)
        _       ← * <~ GiftCards.auth(giftCard = gc, orderPaymentId = Some(payment.id), debit = 50, credit = 0)
        _       ← * <~ GiftCards.auth(giftCard = gc, orderPaymentId = Some(payment.id), debit = 50, credit = 0)
        _       ← * <~ GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = 200, credit = 0)
        gc      ← * <~ GiftCards.findOneById(gc.id).toXor
      } yield gc.value).runT().futureValue.rightVal

      gc.availableBalance must === (0)
      gc.currentBalance must === (200)
    }

    "a Postgres trigger updates the adjustment's availableBalance before insert" in new Fixture {
      val (adj, gc) = (for {
        origin  ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
        gc      ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, originalBalance = 500))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = gc.id, amount = Some(gc.availableBalance)))
        adj     ← * <~ GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = 50, credit = 0)
        adj     ← * <~ GiftCardAdjustments.refresh(adj).toXor
        gc      ← * <~ GiftCards.refresh(gc).toXor
      } yield (adj, gc)).value.run().futureValue.rightVal

      gc.availableBalance must === (450)
      gc.currentBalance must === (450)
      adj.availableBalance must === (gc.availableBalance)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      val (gc, payment) = (for {
        origin  ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
        gc      ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, originalBalance = 500))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = gc.id, amount = Some(gc.availableBalance)))
      } yield (gc, payment)).runT().futureValue.rightVal

      val debits = List(50, 25, 15, 10)
      val adjustments = db.run(DBIO.sequence(debits.map { amount ⇒
        GiftCards.capture(giftCard = gc, orderPaymentId = Some(payment.id), debit = amount, credit = 0)
      })).futureValue

      db.run(DBIO.sequence(adjustments.map { adj ⇒
        GiftCardAdjustments.cancel(adj.rightVal.id)
      })).futureValue

      val finalGc = GiftCards.findOneById(gc.id).run().futureValue.value
      (finalGc.originalBalance, finalGc.availableBalance, finalGc.currentBalance) must === ((500, 500, 500))
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (admin, reason, order) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, reason, order)).runT().futureValue.rightVal
  }
}

