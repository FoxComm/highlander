import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.CartFailures.OrderAlreadyPlaced
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.{NotFoundFailure400, NotFoundFailure404}
import models.cord.Cart
import models.customer.Customers
import models.payment.creditcard._
import payloads.PaymentPayloads.CreditCardPayment
import services.CreditCardManager
import utils.db._
import utils.seeds.Seeds.Factories

class CartCreditCardPaymentsIntegrationTest extends CartPaymentsIntegrationTestBase {

  "POST /v1/orders/:ref/payment-methods/credit-cards" - {
    "succeeds" in new CreditCardFixture {
      val response =
        cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()
      val payments = creditCardPayments(cart)

      payments must have size 1
      payments.head.amount must === (None)
    }

    "successfully replaces an existing card" in new CreditCardFixture {
      cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()
      val newCreditCard = CreditCards.create(creditCard.copy(id = 0, isDefault = false)).gimme
      cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(newCreditCard.id)).mustBeOk()

      val payments = creditCardPayments(cart)
      payments must have size 1
      payments.head.paymentMethodId must === (newCreditCard.id)
    }

    "fails if the cart is not found" in new CreditCardFixture {
      val response = cartsApi("NOPE").payments.creditCard.add(CreditCardPayment(creditCard.id))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the creditCard is not found" in new CreditCardFixture {
      val response = cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(99))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the creditCard is inActive" in new CreditCardFixture {
      CreditCardManager.deleteCreditCard(customer.id, creditCard.id, Some(storeAdmin)).gimme
      val response =
        cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(creditCard.id))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CannotUseInactiveCreditCard(creditCard).description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the order has already been placed" in new CreditCardFixture with Order_Baked {
      val response =
        cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(creditCard.id))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAlreadyPlaced(cart.refNum).description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if customer does not own credit card" in new CreditCardFixture {
      val response = cartsApi(cart.refNum).payments.creditCard
        .add(CreditCardPayment(creditCardOfOtherCustomer.id))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          NotFoundFailure400(CreditCard, creditCardOfOtherCustomer.id).description)
    }
  }

  "DELETE /v1/orders/:ref/payment-methods/credit-cards" - {
    "successfully deletes an existing card" in new CreditCardFixture {
      cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()
      cartsApi(cart.refNum).payments.creditCard.delete().mustBeOk()

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the cart is not found" in new CreditCardFixture {
      val response = cartsApi("NOPE").payments.creditCard.add(CreditCardPayment(creditCard.id))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if there is no creditCard payment" in new CreditCardFixture {
      val response = cartsApi(cart.refNum).payments.creditCard.delete()

      response.status must === (StatusCodes.BadRequest)
      creditCardPayments(cart) mustBe 'empty
    }
  }

  trait CreditCardFixture extends Fixture {
    val (creditCard, creditCardOfOtherCustomer) = (for {
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
      otherCustomer ← * <~ Customers.create(
                         Factories.customer.copy(email = Some("other.customer@email.com")))
      otherCC ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = otherCustomer.id))
    } yield (cc, otherCC)).gimme
  }

}
