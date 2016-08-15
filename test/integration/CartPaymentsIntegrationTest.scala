import java.time.ZonedDateTime

import Extensions._
import akka.http.scaladsl.model.StatusCodes
import util._
import cats.implicits._
import com.stripe.model.DeletedExternalAccount
import failures.CartFailures.OrderAlreadyPlaced
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.GiftCardFailures._
import failures.NotFoundFailure404
import failures.OrderFailures.OrderPaymentNotFoundFailure
import failures.StoreCreditFailures.CustomerHasInsufficientStoreCredit
import models.cord.OrderPayments.scope._
import models.cord._
import models.customer.Customers
import models.location.Addresses
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard._
import models.payment.storecredit._
import models.{Reasons, StoreAdmins}
import org.mockito.Mockito._
import org.mockito.{Matchers ⇒ m}
import org.scalatest.mock.MockitoSugar
import payloads.PaymentPayloads._
import services.{CreditCardManager, Result}
import slick.driver.PostgresDriver.api._
import utils.aliases.stripe._
import utils.db._
import utils.seeds.Seeds.Factories

import scala.concurrent.ExecutionContext.Implicits.global

class CartPaymentsIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with MockitoSugar
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "gift cards" - {
    "POST /v1/orders/:ref/payment-methods/gift-cards" - {
      "succeeds" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.OK)
        val (p :: Nil) = OrderPayments.findAllByCordRef(cart.refNum).gimme.toList

        val payments = giftCardPayments(cart)
        payments must have size 1
        payments.head.amount must === (payload.amount)
      }

      "fails when adding same gift card twice" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
        response.status must === (StatusCodes.OK)

        val secondResponse =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
        secondResponse.status must === (StatusCodes.BadRequest)
        secondResponse.error must === (
            GiftCardPaymentAlreadyAdded(cart.referenceNumber, giftCard.code).description)
      }

      "fails if the cart is not found" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
        val response = POST(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        giftCardPayments(cart) mustBe 'empty
      }

      "fails if the giftCard is not found" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance.some)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(GiftCard, payload.code).description)
        giftCardPayments(cart) mustBe 'empty
      }

      "fails if the giftCard does not have sufficient available balance" in new CartWithGcFixture {
        val requestedAmount = giftCard.availableBalance + 1
        val payload         = GiftCardPayment(code = giftCard.code, amount = requestedAmount.some)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (GiftCardNotEnoughBalance(giftCard, requestedAmount).description)
        giftCardPayments(cart) mustBe 'empty
      }

      "fails if the order has already been placed" in new GiftCardFixture with Order_Baked {
        val payload =
          GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderAlreadyPlaced(cart.refNum).description)
        giftCardPayments(cart) must have size 0
      }

      "fails if the giftCard is inactive" in new CartWithGcFixture {
        GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).gimme
        val payload =
          GiftCardPayment(code = giftCard.code, amount = (giftCard.availableBalance + 1).some)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (GiftCardIsInactive(giftCard).description)
        giftCardPayments(cart) mustBe 'empty
      }

      "fails to add GC with cart status as payment method" in new CartWithGcFixture {
        GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Cart).run().futureValue
        val payload  = GiftCardPayment(code = giftCard.code, amount = Some(15))
        val response = POST(s"v1/orders/${cart.refNum}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (GiftCardMustNotBeCart(giftCard.code).description)
      }
    }

    "PATCH /v1/orders/:ref/payment-methods/gift-cards" - {
      "successfully updates giftCard payment" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
        val create = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
        create.status must === (StatusCodes.OK)

        val update = PATCH(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards",
                           payload.copy(amount = Some(10)))
        update.status must === (StatusCodes.OK)
      }

      "fails if the cart is not found" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
        val response = PATCH(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        giftCardPayments(cart) mustBe 'empty
      }

      "fails if the giftCard is not found" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance.some)
        val response =
          PATCH(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(GiftCard, payload.code).description)
        giftCardPayments(cart) mustBe 'empty
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/gift-cards/:code" - {
      "successfully deletes a giftCard" in new CartWithGcFixture {
        val payload =
          GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
        val create = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
        create.status must === (StatusCodes.OK)

        val response =
          DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")
        val payments = creditCardPayments(cart)

        response.status must === (StatusCodes.OK)
        payments mustBe 'empty
      }

      "fails if the cart is not found" in new CartWithGcFixture {
        val response = DELETE(s"v1/orders/99/payment-methods/gift-cards/123")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Cart, 99).description)
        creditCardPayments(cart) mustBe 'empty
      }

      "fails if the giftCard is not found" in new CartWithGcFixture {
        val response =
          DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards/abc-123")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(GiftCard, "abc-123").description)
        creditCardPayments(cart) mustBe 'empty
      }

      "fails if the giftCard orderPayment is not found" in new CartWithGcFixture {
        val response =
          DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderPaymentNotFoundFailure(GiftCard).description)
        creditCardPayments(cart) mustBe 'empty
      }
    }
  }

  "store credit" - {
    "POST /v1/orders/:ref/payment-methods/store-credit" - {
      "when successful" - {
        "uses store credit records in FIFO cart according to createdAt" in new StoreCreditFixture {
          // ensure 3 & 4 are oldest so 5th should not be used
          StoreCredits
            .filter(_.id === 3)
            .map(_.createdAt)
            .update(ZonedDateTime.now().minusMonths(2).toInstant)
            .run()
            .futureValue
          StoreCredits
            .filter(_.id === 4)
            .map(_.createdAt)
            .update(ZonedDateTime.now().minusMonths(1).toInstant)
            .run()
            .futureValue

          val payload  = StoreCreditPayment(amount = 7500)
          val response = POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)

          response.status must === (StatusCodes.OK)
          val payments = storeCreditPayments(cart)
          payments must have size 2

          val expected =
            payments.sortBy(_.paymentMethodId).map(p ⇒ (p.paymentMethodId, p.amount)).toList
          expected must === (List((3, Some(5000)), (4, Some(2500))))
        }

        "only uses active store credit" in new StoreCreditFixture {
          // inactive 1 and 2
          StoreCredits
            .filter(_.id === 1)
            .map(_.state)
            .update(StoreCredit.Canceled)
            .run()
            .futureValue
          StoreCredits.filter(_.id === 2).map(_.availableBalance).update(0).run().futureValue

          val payload  = StoreCreditPayment(amount = 7500)
          val response = POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)

          response.status must === (StatusCodes.OK)
          val payments = storeCreditPayments(cart)
          payments.map(_.paymentMethodId) must contain noneOf (1, 2)
          payments must have size 2
        }

        "adding store credit should remove previous cart payments" in new StoreCreditFixture {
          val payload = StoreCreditPayment(amount = 7500)
          val createdResponse =
            POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)
          val createdPayments = storeCreditPayments(cart)

          createdResponse.status must === (StatusCodes.OK)
          createdPayments must have size 2

          val createdPaymentIds = createdPayments.map(_.id).toList
          val editedResponse =
            POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)
          val editedPayments = storeCreditPayments(cart)

          editedResponse.status must === (StatusCodes.OK)
          editedPayments must have size 2
          editedPayments.map(_.id) mustNot contain theSameElementsAs createdPaymentIds
        }
      }

      "fails if the cart is not found" in new Fixture {
        val notFound = cart.copy(referenceNumber = "ABC123")
        val payload  = StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${notFound.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Cart, notFound.refNum).description)
        storeCreditPayments(cart) mustBe 'empty
      }

      "fails if the customer has no active store credit" in new Fixture {
        val payload  = StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        val error = CustomerHasInsufficientStoreCredit(customer.id, 0, 50).description
        response.error must === (error)
        storeCreditPayments(cart) mustBe 'empty
      }

      "fails if the customer has insufficient available store credit" in new StoreCreditFixture {
        val payload  = StoreCreditPayment(amount = 25100)
        val response = POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        val has = storeCredits.map(_.availableBalance).sum
        val error =
          CustomerHasInsufficientStoreCredit(customer.id, has, payload.amount).description
        response.error must === (error)
        storeCreditPayments(cart) mustBe 'empty
      }

      "fails if the order has already been placed" in new StoreCreditFixture with Order_Baked {
        val payload  = StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderAlreadyPlaced(cart.refNum).description)
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/store-credit" - {
      "successfully deletes all store credit payments" in new StoreCreditFixture {
        val payload = StoreCreditPayment(amount = 75)
        val create  = POST(s"v1/orders/${cart.refNum}/payment-methods/store-credit", payload)

        create.status must === (StatusCodes.OK)

        val response = DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/store-credit")

        response.status must === (StatusCodes.OK)
        storeCreditPayments(cart) mustBe 'empty
      }
    }
  }

  "credit cards" - {
    "POST /v1/orders/:ref/payment-methods/credit-cards" - {
      "succeeds" in new CreditCardFixture {
        val payload = CreditCardPayment(creditCard.id)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards", payload)
        val payments = creditCardPayments(cart)

        response.status must === (StatusCodes.OK)
        payments must have size 1
        payments.head.amount must === (None)
      }

      "successfully replaces an existing card" in new CreditCardFixture {
        val first = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards",
                         CreditCardPayment(creditCard.id))
        first.status must === (StatusCodes.OK)

        val newCreditCard = CreditCards.create(creditCard.copy(id = 0, isDefault = false)).gimme
        val second = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards",
                          CreditCardPayment(newCreditCard.id))
        second.status must === (StatusCodes.OK)

        val payments = creditCardPayments(cart)
        payments must have size 1
        payments.head.paymentMethodId must === (newCreditCard.id)
      }

      "fails if the cart is not found" in new CreditCardFixture {
        val payload  = CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/99/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Cart, 99).description)
        creditCardPayments(cart) mustBe 'empty
      }

      "fails if the creditCard is not found" in new CreditCardFixture {
        val payload = CreditCardPayment(99)
        val response =
          POST(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(CreditCard, 99).description)
        creditCardPayments(cart) mustBe 'empty
      }

      "fails if the creditCard is inActive" in new CreditCardFixture {
        reset(stripeApiMock)

        when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

        when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

        when(stripeApiMock.deleteExternalAccount(m.any()))
          .thenReturn(Result.good(new DeletedExternalAccount))

        CreditCardManager.deleteCreditCard(customer.id, creditCard.id, Some(storeAdmin)).gimme
        val response = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards",
                            CreditCardPayment(creditCard.id))

        response.status must === (StatusCodes.BadRequest)
        response.error must === (CannotUseInactiveCreditCard(creditCard).description)
        creditCardPayments(cart) mustBe 'empty
      }

      "fails if the order has already been placed" in new CreditCardFixture with Order_Baked {
        val response = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards",
                            CreditCardPayment(creditCard.id))

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderAlreadyPlaced(cart.refNum).description)
        creditCardPayments(cart) must have size 0
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/credit-cards" - {
      "successfully deletes an existing card" in new CreditCardFixture {
        val create = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards",
                          CreditCardPayment(creditCard.id))
        create.status must === (StatusCodes.OK)

        val response = DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards")
        val payments = creditCardPayments(cart)

        response.status must === (StatusCodes.OK)
        payments mustBe 'empty
      }

      "fails if the cart is not found" in new CreditCardFixture {
        val payload  = CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/99/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Cart, 99).description)
        creditCardPayments(cart) mustBe 'empty
      }

      "fails if there is no creditCard payment" in new CreditCardFixture {
        val response = DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/credit-cards")
        val payments = creditCardPayments(cart)

        response.status must === (StatusCodes.BadRequest)
        payments mustBe 'empty
      }
    }
  }

  def paymentsFor(cart: Cart, pmt: PaymentMethod.Type): Seq[OrderPayment] = {
    OrderPayments.filter(_.cordRef === cart.refNum).byType(pmt).gimme
  }

  def creditCardPayments(cart: Cart) =
    paymentsFor(cart, PaymentMethod.CreditCard)
  def giftCardPayments(cart: Cart) =
    paymentsFor(cart, PaymentMethod.GiftCard)
  def storeCreditPayments(cart: Cart) =
    paymentsFor(cart, PaymentMethod.StoreCredit)

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed

  trait GiftCardFixture extends StoreAdmin_Seed {
    val giftCard = (for {
      reason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = storeAdmin.id))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
  }

  trait CartWithGcFixture extends Fixture with GiftCardFixture

  trait StoreCreditFixture extends Fixture {
    val storeCredits = (for {
      reason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = storeAdmin.id))
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

  trait CreditCardFixture extends Fixture {
    val creditCard = CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).gimme
  }
}
