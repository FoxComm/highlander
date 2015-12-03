package models

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

class StoreCreditAdjustmentIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "StoreCreditAdjustment" - {
    "debit must be greater than zero" in new Fixture {
      val (sc, payment) = (for {
        origin  ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        sc      ← * <~ StoreCredits.create(Factories.storeCredit.copy(originId = origin.id))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = sc.id, amount = Some(25)))
      } yield (sc, payment)).runT().futureValue.rightVal

      val adjustments = Table(
        "adjustments",
        StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(payment.id), amount = -1),
        StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(payment.id), amount = 0)
      )

      forAll(adjustments) { adjustment ⇒
        val failure = adjustment.run().futureValue.leftVal
        failure.getMessage must include( """violates check constraint "valid_debit"""")
      }
    }

    "updates the StoreCredit's currentBalance and availableBalance after insert" in new Fixture {
      val sc = (for {
        origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        sc     ← * <~ StoreCredits.create(Factories.storeCredit.copy(originalBalance = 500, originId = origin.id))
        pay    ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = sc.id, amount = Some(500)))
        _      ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 50)
        _      ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 25)
        _      ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 15)
        _      ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 10)
        _      ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 100)
        _      ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 50)
        _      ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 50)
        _      ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 200)
        sc     ← * <~ StoreCredits.findOneById(sc.id).toXor
      } yield sc.value).runT().futureValue.rightVal

      sc.availableBalance must === (0)
      sc.currentBalance must === (200)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      val (sc, payment) = (for {
        origin  ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        sc      ← * <~ StoreCredits.create(Factories.storeCredit.copy(originalBalance = 500, originId = origin.id))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = sc.id, amount = Some(500)))
      } yield (sc, payment)).runT().futureValue.rightVal

      val debits = List(50, 25, 15, 10)
      val adjustments = db.run(DBIO.sequence(debits.map { amount ⇒
        StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(payment.id), amount = amount)
      })).futureValue

      db.run(DBIO.sequence(adjustments.map { adj ⇒
        StoreCreditAdjustments.cancel(adj.rightVal.id)
      })).futureValue

      val finalSc = StoreCredits.findOneById(sc.id).run().futureValue.value
      (finalSc.originalBalance, finalSc.availableBalance, finalSc.currentBalance) must === ((500, 500, 500))
    }
  }

  trait Fixture {
    val (admin, customer, reason, order) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, reason, order)).runT().futureValue.rightVal
  }
}

