package models

import cats.implicits._
import models.cord.OrderPayments
import models.payment.storecredit._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class StoreCreditAdjustmentIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with TestObjectContext {

  import api._

  "StoreCreditAdjustment" - {
    "debit must be greater than zero" in new Fixture {
      val (sc, payment) = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(
                Factories.storeCredit.copy(originId = origin.id, accountId = customer.accountId))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment
                       .copy(cordRef = cart.refNum, paymentMethodId = sc.id, amount = Some(25)))
      } yield (sc, payment)).gimme

      val adjustments = Table(
          "adjustments",
          StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = -1),
          StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = 0)
      )

      forAll(adjustments) { adjustment ⇒
        val failure = adjustment.gimmeFailures
        failure.getMessage must include("""violates check constraint "valid_debit"""")
      }
    }

    "updates the StoreCredit's currentBalance and availableBalance before insert" in new Fixture {
      val sc = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(
                Factories.storeCredit.copy(originalBalance = 500,
                                           originId = origin.id,
                                           accountId = customer.accountId))
        pay ← * <~ OrderPayments.create(Factories.giftCardPayment
                   .copy(cordRef = cart.refNum, paymentMethodId = sc.id, amount = Some(500)))
        _  ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = pay.id, amount = 100)
        _  ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = pay.id, amount = 50)
        _  ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = pay.id, amount = 50)
        _  ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = pay.id, amount = 50)
        _  ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = pay.id, amount = 25)
        _  ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = pay.id, amount = 15)
        sc ← * <~ StoreCredits.findOneById(sc.id)
      } yield sc.value).gimme

      sc.availableBalance must === (500 - 50 - 25 - 15)
      sc.currentBalance must === (500 - 50 - 25 - 15)
    }

    "a Postgres trigger updates the adjustment's availableBalance before insert" in new Fixture {
      val (adj, sc) = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(
                Factories.storeCredit.copy(originalBalance = 500,
                                           originId = origin.id,
                                           accountId = customer.accountId))
        pay ← * <~ OrderPayments.create(Factories.giftCardPayment
                   .copy(cordRef = cart.refNum, paymentMethodId = sc.id, amount = Some(500)))
        auth ← * <~ StoreCredits.auth(storeCredit = sc, orderPaymentId = pay.id, amount = 50)
        adj  ← * <~ StoreCredits.capture(storeCredit = sc, orderPaymentId = pay.id, amount = 50)
        adj  ← * <~ StoreCreditAdjustments.refresh(adj)
        sc   ← * <~ StoreCredits.refresh(sc)
      } yield (adj, sc)).value.gimme

      sc.availableBalance must === (450)
      sc.currentBalance must === (450)
      adj.availableBalance must === (sc.availableBalance)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      val (sc, payment) = (for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
        sc ← * <~ StoreCredits.create(
                Factories.storeCredit.copy(originalBalance = 500,
                                           originId = origin.id,
                                           accountId = customer.accountId))
        payment ← * <~ OrderPayments.create(Factories.giftCardPayment
                       .copy(cordRef = cart.refNum, paymentMethodId = sc.id, amount = Some(500)))
      } yield (sc, payment)).gimme

      val debits = List(50, 25, 15, 10)
      val auths = DbResultT
        .seqCollectFailures(debits.map { amount ⇒
          StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = amount)
        })
        .gimme

      auths.map { adj ⇒
        StoreCreditAdjustments.cancel(adj.id).gimme
      }

      val finalSc = StoreCredits.findOneById(sc.id).gimme.value
      (finalSc.originalBalance, finalSc.availableBalance, finalSc.currentBalance) must === (
          (500, 500, 500))
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed {
    val reason = Reasons.create(Factories.reason(storeAdmin.accountId)).gimme
  }
}
