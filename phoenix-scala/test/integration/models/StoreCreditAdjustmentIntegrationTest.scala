package models

import cats.implicits
import models.cord.OrderPayments
import models.payment.storecredit._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import payloads.PaymentPayloads._
import responses.StoreCreditResponse
import testutils._
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import utils.db._

class StoreCreditAdjustmentIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with ApiFixtureHelpers
    with ApiFixtures
    with TestObjectContext {

  import api._

  "StoreCreditAdjustment" - {
    "debit must be greater than zero" in new Fixture {
      val adjustments = Table(
          "adjustments",
          auth(-1),
          auth(0)
      )

      forAll(adjustments) { adjustment ⇒
        val failure = adjustment.run().futureValue.leftVal
        failure.getMessage must include("""violates check constraint "valid_debit"""")
      }
    }

    "updates the StoreCredit's currentBalance and availableBalance before insert" in new Fixture {
      val sc = (for {
        _  ← * <~ auth(50)
        _  ← * <~ auth(25)
        _  ← * <~ auth(15)
        _  ← * <~ auth(10)
        _  ← * <~ capture(100)
        _  ← * <~ auth(100)
        _  ← * <~ auth(50)
        _  ← * <~ auth(50)
        _  ← * <~ capture(200)
        _  ← * <~ auth(200)
        sc ← * <~ StoreCredits.refresh(storeCreditModel)
      } yield sc).gimme

      sc.availableBalance must === (0)
      sc.currentBalance must === (200)
    }

    "a Postgres trigger updates the adjustment's availableBalance before insert" in new Fixture {
      val (adj, sc) = (for {
        _   ← * <~ auth(50)
        adj ← * <~ capture(50)
        sc  ← * <~ StoreCredits.refresh(storeCreditModel)
      } yield (adj, sc)).gimme

      sc.availableBalance must === (450)
      sc.currentBalance must === (450)
      adj.availableBalance must === (sc.availableBalance)
    }

    "cancels an adjustment and removes its effect on current/available balances" in new Fixture {
      val debits = List(50, 25, 15, 10)
      val adjustments = {
        val captures = debits.map { amount ⇒
          for {
            _   ← * <~ auth(amount)
            adj ← * <~ capture(amount)
          } yield adj
        }
        DbResultT.sequence(captures).gimme
      }

      DBIO.sequence(adjustments.map(adj ⇒ StoreCreditAdjustments.cancel(adj.id))).gimme

      val finalSc = StoreCredits.refresh(storeCreditModel).gimme
      (finalSc.originalBalance, finalSc.availableBalance, finalSc.currentBalance) must === (
          (500, 500, 500))
    }
  }

  trait Fixture extends StoreAdmin_Seed with Reason_Baked {
    val customerId = api_newCustomer().id
    val cartRef    = api_newCustomerCart(customerId).referenceNumber

    val storeCreditResponse = customersApi(customerId).payments.storeCredit
      .create(CreateManualStoreCredit(amount = 500, reasonId = reason.id))
      .as[StoreCreditResponse.Root]

    cartsApi(cartRef).payments.storeCredit.add(StoreCreditPayment(amount = 500)).mustBeOk()

    val storeCreditModel = StoreCredits.mustFindById400(storeCreditResponse.id).gimme
    val payment          = OrderPayments.findAllByCordRef(cartRef).gimme.headOption.value

    def auth(amount: Int) =
      StoreCredits
        .auth(storeCredit = storeCreditModel, orderPaymentId = Some(payment.id), amount = amount)

    def capture(amount: Int) =
      StoreCredits.capture(storeCredit = storeCreditModel,
                           orderPaymentId = Some(payment.id),
                           amount = amount)
  }
}
