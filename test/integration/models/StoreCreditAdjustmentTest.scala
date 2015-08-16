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
        (StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = -1)),
        (StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = 0))
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
        _ ← StoreCredits.capture(storeCredit = sc, orderPaymentId = payment.id, amount = 50)
        _ ← StoreCredits.capture(storeCredit = sc, orderPaymentId = payment.id, amount = 25)
        _ ← StoreCredits.capture(storeCredit = sc, orderPaymentId = payment.id, amount = 15)
        _ ← StoreCredits.capture(storeCredit = sc, orderPaymentId = payment.id, amount = 10)
        _ ← StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = 100)
        _ ← StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = 50)
        _ ← StoreCredits.auth(storeCredit = sc, orderPaymentId = payment.id, amount = 50)
        _ ← StoreCredits.capture(storeCredit = sc, orderPaymentId = payment.id, amount = 200)
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

