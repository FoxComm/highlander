package models

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils.Seeds.Factories

class StoreCreditAdjustmentTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "StoreCreditAdjustment" - {
    "debit must be greater than zero" in new Fixture {
      val (sc, payment) = (for {
        origin ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id))
        sc ← StoreCredits.save(Factories.storeCredit.copy(originId = origin.id))
        payment ← OrderPayments.save(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = sc.id))
      } yield (sc, payment)).run().futureValue

      val adjustments = Table(
        ("adjustments"),
        (StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = -1, capture = false)),
        (StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 0, capture = false))
      )

      forAll(adjustments) { adjustment ⇒
        val failure = adjustment.run().failed.futureValue
        failure.getMessage must include( """violates check constraint "valid_debit"""")
      }
    }

    "updates the StoreCredit's currentBalance and availableBalance after insert" in new Fixture {
      val sc = (for {
        origin ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id))
        sc ← StoreCredits.save(Factories.storeCredit.copy(originalBalance = 500, originId = origin.id))
        payment ← OrderPayments.save(Factories.giftCardPayment.copy(orderId = order.id,
          paymentMethodId = sc.id))
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 50, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 25, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 15, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 10, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 100, capture = false)
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 50, capture = false)
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 50, capture = false)
        _ ← StoreCredits.debit(storeCredit = sc, orderPaymentId = payment.id, debit = 200, capture = true)
        sc ← StoreCredits.findById(sc.id)
      } yield sc.get).run().futureValue

      sc.availableBalance must === (0)
      sc.currentBalance must === (200)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (admin, customer, reason, order) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, reason, order)).run().futureValue
  }
}

