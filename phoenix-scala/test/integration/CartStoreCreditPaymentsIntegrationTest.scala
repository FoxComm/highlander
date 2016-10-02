import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.CartFailures.OrderAlreadyPlaced
import failures.NotFoundFailure404
import failures.StoreCreditFailures.CustomerHasInsufficientStoreCredit
import models.Reasons
import models.cord.Cart
import models.payment.storecredit._
import payloads.PaymentPayloads.StoreCreditPayment
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.seeds.Seeds.Factories

class CartStoreCreditPaymentsIntegrationTest extends CartPaymentsIntegrationTestBase {

  "POST /v1/orders/:ref/payment-methods/store-credit" - {
    "when successful" - {
      "uses store credit records in FIFO cart according to createdAt" in new StoreCreditFixture {
        // ensure 3 & 4 are oldest so 5th should not be used
        StoreCredits
          .filter(_.id === 3)
          .map(_.createdAt)
          .update(ZonedDateTime.now().minusMonths(2).toInstant)
          .gimme
        StoreCredits
          .filter(_.id === 4)
          .map(_.createdAt)
          .update(ZonedDateTime.now().minusMonths(1).toInstant)
          .gimme

        cartsApi(cart.refNum).payments.storeCredit
          .add(StoreCreditPayment(amount = 7500))
          .mustBeOk()

        val expected = storeCreditPayments(cart).map(p ⇒ (p.paymentMethodId, p.amount))
        expected must contain theSameElementsAs Seq((3, Some(5000)), (4, Some(2500)))
      }

      "only uses active store credit" in new StoreCreditFixture {
        // inactive 1 and 2
        StoreCredits.filter(_.id === 1).map(_.state).update(StoreCredit.Canceled).run().futureValue
        StoreCredits.filter(_.id === 2).map(_.availableBalance).update(0).run().futureValue

        cartsApi(cart.refNum).payments.storeCredit
          .add(StoreCreditPayment(amount = 7500))
          .mustBeOk()

        val payments = storeCreditPayments(cart)
        payments.map(_.paymentMethodId) must contain noneOf (1, 2)
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
      val payload  = StoreCreditPayment(amount = 50)
      val response = cartsApi("NOPE").payments.storeCredit.add(payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
      storeCreditPayments(cart) mustBe 'empty
    }

    "fails if the customer has no active store credit" in new Fixture {
      val payload  = StoreCreditPayment(amount = 50)
      val response = cartsApi(cart.refNum).payments.storeCredit.add(payload)

      response.status must === (StatusCodes.BadRequest)
      val error = CustomerHasInsufficientStoreCredit(customer.id, 0, 50).description
      response.error must === (error)
      storeCreditPayments(cart) mustBe 'empty
    }

    "fails if the customer has insufficient available store credit" in new StoreCreditFixture {
      val payload  = StoreCreditPayment(amount = 25100)
      val response = cartsApi(cart.refNum).payments.storeCredit.add(payload)

      response.status must === (StatusCodes.BadRequest)
      val has   = storeCredits.map(_.availableBalance).sum
      val error = CustomerHasInsufficientStoreCredit(customer.id, has, payload.amount).description
      response.error must === (error)
      storeCreditPayments(cart) mustBe 'empty
    }

    "fails if the order has already been placed" in new StoreCreditFixture with Order_Baked {
      val payload  = StoreCreditPayment(amount = 50)
      val response = cartsApi(cart.refNum).payments.storeCredit.add(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAlreadyPlaced(cart.refNum).description)
    }
  }

  "DELETE /v1/orders/:ref/payment-methods/store-credit" - {
    "successfully deletes all store credit payments" in new StoreCreditFixture {
      cartsApi(cart.refNum).payments.storeCredit.add(StoreCreditPayment(amount = 75)).mustBeOk()
      cartsApi(cart.refNum).payments.storeCredit.delete().mustBeOk()

      storeCreditPayments(cart) mustBe 'empty
    }
  }

  trait StoreCreditFixture extends Fixture {
    val storeCredits = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
      _ ← * <~ StoreCreditManuals.createAll((1 to 5).map { _ ⇒
           StoreCreditManual(adminId = storeAdmin.id, reasonId = reason.id)
         })
      _ ← * <~ StoreCredits.createAll((1 to 5).map { i ⇒
           Factories.storeCredit.copy(state = StoreCredit.Active,
                                      customerId = customer.id,
                                      originId = i)
         })
      storeCredits ← * <~ StoreCredits.findAllByCustomerId(customer.id).result
    } yield storeCredits).gimme
  }

}
