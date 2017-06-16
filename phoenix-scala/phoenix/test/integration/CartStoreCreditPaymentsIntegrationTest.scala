import java.time.ZonedDateTime

import core.failures.NotFoundFailure404
import phoenix.failures.CartFailures.OrderAlreadyPlaced
import phoenix.failures.StoreCreditFailures.CustomerHasInsufficientStoreCredit
import phoenix.models.Reasons
import phoenix.models.cord.Cart
import phoenix.models.payment.storecredit._
import phoenix.payloads.PaymentPayloads.StoreCreditPayment
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import testutils._
import core.db._

class CartStoreCreditPaymentsIntegrationTest extends CartPaymentsIntegrationTestBase {

  "POST /v1/carts/:ref/payment-methods/store-credit" - {
    "when successful" - {
      "uses store credit records in FIFO cart according to createdAt" in new StoreCreditFixture {
        // ensure 3 & 4 are oldest so 5th should not be used
        StoreCredits
          .filter(_.id === storeCredits(3 - 1).id)
          .map(_.createdAt)
          .update(ZonedDateTime.now().minusMonths(2).toInstant)
          .gimme
        StoreCredits
          .filter(_.id === storeCredits(4 - 1).id)
          .map(_.createdAt)
          .update(ZonedDateTime.now().minusMonths(1).toInstant)
          .gimme

        cartsApi(cart.refNum).payments.storeCredit
          .add(StoreCreditPayment(amount = 7500))
          .mustBeOk()

        val expected = storeCreditPayments(cart).map(p ⇒ (p.paymentMethodId, p.amount))
        expected must contain theSameElementsAs Seq((storeCredits(3 - 1).id, Some(5000)),
                                                    (storeCredits(4 - 1).id, Some(2500)))
      }

      "only uses active store credit" in new StoreCreditFixture {
        // inactive 1 and 2
        StoreCredits
          .filter(_.id === storeCredits(1 - 1).id)
          .map(_.state)
          .update(StoreCredit.Canceled)
          .run()
          .futureValue
        StoreCredits
          .filter(_.id === storeCredits(2 - 1).id)
          .map(_.availableBalance)
          .update(0)
          .run()
          .futureValue

        cartsApi(cart.refNum).payments.storeCredit
          .add(StoreCreditPayment(amount = 7500))
          .mustBeOk()

        val payments = storeCreditPayments(cart)
        payments.map(_.paymentMethodId) must contain noneOf (
          storeCredits(1 - 1).id,
          storeCredits(2 - 1).id
        )
        payments must have size 2
      }

      "adding store credit should remove previous cart payments" in new StoreCreditFixture {
        val payload = StoreCreditPayment(amount = 7500)
        cartsApi(cart.refNum).payments.storeCredit.add(payload).mustBeOk()
        val createdPayments = storeCreditPayments(cart)

        createdPayments must have size 2

        val createdPaymentIds = createdPayments.map(_.id).toList
        cartsApi(cart.refNum).payments.storeCredit.add(payload).mustBeOk()
        val editedPayments = storeCreditPayments(cart)

        editedPayments must have size 2
        editedPayments.map(_.id) mustNot contain theSameElementsAs createdPaymentIds
      }
    }

    "fails if the cart is not found" in new Fixture {
      cartsApi("NOPE").payments.storeCredit
        .add(StoreCreditPayment(amount = 50))
        .mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      storeCreditPayments(cart) mustBe 'empty
    }

    "fails if the customer has no active store credit" in new Fixture {
      cartsApi(cart.refNum).payments.storeCredit
        .add(StoreCreditPayment(amount = 50))
        .mustFailWith400(CustomerHasInsufficientStoreCredit(customer.accountId, 0, 50))

      storeCreditPayments(cart) mustBe 'empty
    }

    "fails if the customer has insufficient available store credit" in new StoreCreditFixture {
      val failure = CustomerHasInsufficientStoreCredit(customer.accountId,
                                                       storeCredits.map(_.availableBalance).sum,
                                                       StoreCreditPayment(amount = 25100).amount)
      cartsApi(cart.refNum).payments.storeCredit
        .add(StoreCreditPayment(amount = 25100))
        .mustFailWith400(failure)

      storeCreditPayments(cart) mustBe 'empty
    }

    "fails if the order has already been placed" in new StoreCreditFixture with Order_Baked {
      cartsApi(cart.refNum).payments.storeCredit
        .add(StoreCreditPayment(amount = 50))
        .mustFailWith400(OrderAlreadyPlaced(cart.refNum))
    }
  }

  "DELETE /v1/carts/:ref/payment-methods/store-credit" - {
    "successfully deletes all store credit payments" in new StoreCreditFixture {
      cartsApi(cart.refNum).payments.storeCredit.add(StoreCreditPayment(amount = 75)).mustBeOk()
      cartsApi(cart.refNum).payments.storeCredit.delete().mustBeOk()

      storeCreditPayments(cart) mustBe 'empty
    }
  }

  trait StoreCreditFixture extends Fixture {
    val storeCredits = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      storeCreditManuals ← * <~ StoreCreditManuals.createAllReturningIds((1 to 5).map { _ ⇒
                            StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id)
                          })
      _ ← * <~ StoreCredits.createAll(storeCreditManuals.map { scmId ⇒
           Factories.storeCredit.copy(state = StoreCredit.Active,
                                      accountId = customer.accountId,
                                      originId = scmId)
         })
      storeCredits ← * <~ StoreCredits.findAllByAccountId(customer.accountId).result
    } yield storeCredits).gimme
  }

}
