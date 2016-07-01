package models

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customers
import models.order.{OrderPayments, Orders}
import models.payment.storecredit._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class StoreCreditAdjustmentIntegrationTest extends IntegrationTestBase {

  import api._

  "StoreCreditAdjustment" - {
    "debit must be greater than zero" in new Fixture {
      val (sc, payment) = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(Factories.storeCredit.copy(originId = origin.id))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment
                       .copy(orderRef = order.refNum, paymentMethodId = sc.id, amount = Some(25)))
      } yield (sc, payment)).gimme

      val adjustments = Table(
          "adjustments",
          StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(payment.id), amount = -1),
          StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(payment.id), amount = 0)
      )

      forAll(adjustments) { adjustment ⇒
        val failure = adjustment.run().futureValue.leftVal
        failure.getMessage must include("""violates check constraint "valid_debit"""")
      }
    }

    "updates the StoreCredit's currentBalance and availableBalance before insert" in new Fixture {
      val sc = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(
                Factories.storeCredit.copy(originalBalance = 500, originId = origin.id))
        pay ← * <~ OrderPayments.create(Factories.giftCardPayment
                   .copy(orderRef = order.refNum, paymentMethodId = sc.id, amount = Some(500)))
        _ ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 50)
        _ ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 25)
        _ ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 15)
        _ ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 10)
        _ ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 100)
        _ ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 50)
        _ ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = Some(pay.id), amount = 50)
        _ ← * <~ StoreCredits.capture(storeCredit = sc,
                                      orderPaymentId = Some(pay.id),
                                      amount = 200)
        sc ← * <~ StoreCredits.findOneById(sc.id)
      } yield sc.value).gimme

      sc.availableBalance must === (0)
      sc.currentBalance must === (200)
    }

    "a Postgres trigger updates the adjustment's availableBalance before insert" in new Fixture {
      val (adj, sc) = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(
                Factories.storeCredit.copy(originalBalance = 500, originId = origin.id))
        pay ← * <~ OrderPayments.create(Factories.giftCardPayment
                   .copy(orderRef = order.refNum, paymentMethodId = sc.id, amount = Some(500)))
        adj ← * <~ StoreCredits.capture(storeCredit = sc,
                                        orderPaymentId = Some(pay.id),
                                        amount = 50)
        adj ← * <~ StoreCreditAdjustments.refresh(adj)
        sc  ← * <~ StoreCredits.refresh(sc)
      } yield (adj, sc)).value.gimme

      sc.availableBalance must === (450)
      sc.currentBalance must === (450)
      adj.availableBalance must === (sc.availableBalance)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      val (sc, payment) = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(
                Factories.storeCredit.copy(originalBalance = 500, originId = origin.id))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment
                       .copy(orderRef = order.refNum, paymentMethodId = sc.id, amount = Some(500)))
      } yield (sc, payment)).gimme

      val debits = List(50, 25, 15, 10)
      val adjustments = DbResultT
        .sequence(debits.map { amount ⇒
          StoreCredits.capture(storeCredit = sc,
                               orderPaymentId = Some(payment.id),
                               amount = amount)
        })
        .gimme

      DBIO
        .sequence(adjustments.map { adj ⇒
          StoreCreditAdjustments.cancel(adj.id)
        })
        .gimme

      val finalSc = StoreCredits.findOneById(sc.id).run().futureValue.value
      (finalSc.originalBalance, finalSc.availableBalance, finalSc.currentBalance) must === (
          (500, 500, 500))
    }
  }

  trait Fixture {
    val (admin, customer, reason, order) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, reason, order)).gimme
  }
}
