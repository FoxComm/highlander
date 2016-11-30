import failures.CartFailures.OrderAlreadyPlaced
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.OrderFailures.OrderPaymentNotFoundFailure
import failures.{NotFoundFailure400, NotFoundFailure404}
import models.account._
import models.cord.Cart
import models.customer._
import models.payment.creditcard._
import payloads.PaymentPayloads.CreditCardPayment
import services.CreditCardManager
import testutils._
import utils.db._
import utils.seeds.Seeds.Factories

class CartCreditCardPaymentsIntegrationTest extends CartPaymentsIntegrationTestBase {

  "POST /v1/carts/:ref/payment-methods/credit-cards" - {
    "succeeds" in new CreditCardFixture {
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
      cartsApi("NOPE").payments.creditCard
        .add(CreditCardPayment(creditCard.id))
        .mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the creditCard is not found" in new CreditCardFixture {
      cartsApi(cart.refNum).payments.creditCard
        .add(CreditCardPayment(99))
        .mustFailWith400(NotFoundFailure400(CreditCard, 99))

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the creditCard is inActive" in new CreditCardFixture {
      CreditCardManager.deleteCreditCard(customer.accountId, creditCard.id, Some(storeAdmin)).gimme

      cartsApi(cart.refNum).payments.creditCard
        .add(CreditCardPayment(creditCard.id))
        .mustFailWith400(CannotUseInactiveCreditCard(creditCard))

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the order has already been placed" in new CreditCardFixture with Order_Baked {
      cartsApi(cart.refNum).payments.creditCard
        .add(CreditCardPayment(creditCard.id))
        .mustFailWith400(OrderAlreadyPlaced(cart.refNum))

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if customer does not own credit card" in new CreditCardFixture {
      cartsApi(cart.refNum).payments.creditCard
        .add(CreditCardPayment(creditCardOfOtherCustomer.id))
        .mustFailWith400(NotFoundFailure400(CreditCard, creditCardOfOtherCustomer.id))
    }
  }

  "DELETE /v1/carts/:ref/payment-methods/credit-cards" - {
    "successfully deletes an existing card" in new CreditCardFixture {
      cartsApi(cart.refNum).payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()
      cartsApi(cart.refNum).payments.creditCard.delete().mustBeOk()

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the cart is not found" in new CreditCardFixture {
      cartsApi("NOPE").payments.creditCard
        .add(CreditCardPayment(creditCard.id))
        .mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if there is no creditCard payment" in new CreditCardFixture {
      cartsApi(cart.refNum).payments.creditCard
        .delete()
        .mustFailWith400(OrderPaymentNotFoundFailure(CreditCard))

      creditCardPayments(cart) mustBe 'empty
    }
  }

  trait CreditCardFixture extends Fixture {
    val (creditCard, creditCardOfOtherCustomer) = (for {
      cc           ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId))
      otherAccount ← * <~ Accounts.create(Account())
      otherCustomer ← * <~ Users.create(
                         Factories.customer.copy(accountId = otherAccount.id,
                                                 email = Some("other.customer@email.com")))
      _ ← * <~ CustomersData.create(
             CustomerData(userId = otherCustomer.id, accountId = otherAccount.id))
      otherCC ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = otherAccount.id))
    } yield (cc, otherCC)).gimme
  }

}
