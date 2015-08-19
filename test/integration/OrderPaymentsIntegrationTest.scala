import akka.http.scaladsl.model.StatusCodes
import models._
import org.joda.time.DateTime
import org.scalatest.time.{Milliseconds, Seconds, Span}
import payloads.{UpdateOrderPayload, CreateAddressPayload, CreateCreditCard}
import responses.{AdminNotes, FullOrder}
import services.{CustomerHasNoStoreCredit, OrderNotFoundFailure, CustomerManager, CannotUseInactiveCreditCard,
NotFoundFailure, GiftCardNotEnoughBalance, GiftCardNotFoundFailure, NoteManager}
import util.{IntegrationTestBase, StripeSupport}
import utils.Seeds.Factories
import utils._
import slick.driver.PostgresDriver.api._
import Order._

class OrderPaymentsIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import org.json4s.jackson.JsonMethods._
  import Extensions._

  "gift cards" - {
    "when added as a payment method" - {
      "succeeds" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must ===(StatusCodes.OK)
        val (p :: Nil) = OrderPayments.findAllByOrderId(order.id).result.run().futureValue.toList

        p.paymentMethodType must ===(PaymentMethod.GiftCard)
        p.amount must ===((Some(payload.amount)))
      }

      "fails if the order is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
      }

      "fails if the giftCard is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response).get.head must === (GiftCardNotFoundFailure(payload.code).description.head)
      }

      "fails if the giftCard does not have sufficient available balance" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance + 1)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        parseErrors(response).get.head must === (GiftCardNotEnoughBalance(giftCard, payload.amount).description.head)
      }
    }
  }

  "store credit" - {
    "when added as a payment method" - {
      "fails if the order is not found" in new Fixture {
        val notFound = order.copy(referenceNumber = "ABC123")
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${notFound.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response).get.head must === (OrderNotFoundFailure(notFound).description.head)
      }

      "fails if the customer has no store credit" in new Fixture {
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        parseErrors(response).get.head must === (CustomerHasNoStoreCredit(customer.id).description.head)
      }
    }
  }

  "deleting a payment" - {
    "successfully deletes" in new GiftCardFixture {
      val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
      val payment = services.OrderUpdater.addGiftCard(order.referenceNumber, payload).futureValue.get

      val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/${payment.id}")
      response.status must ===(StatusCodes.NoContent)

      val payments = OrderPayments.findAllByOrderId(order.id).result.run().futureValue
      payments must have size (0)
    }

    "fails if the order is not found" in new GiftCardFixture {
      val response = DELETE(s"v1/orders/ABCAYXADSF/payment-methods/1")
      response.status must ===(StatusCodes.NotFound)
    }

    "fails if the payment is not found" in new GiftCardFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/1")
      response.status must ===(StatusCodes.NotFound)
    }
  }

  "credit cards" - {
    "when added as a payment method" - {
      "succeeds" in new CreditCardFixture {
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")

        response.status must === (StatusCodes.OK)
        val (p :: Nil) = OrderPayments.findAllByOrderId(order.id).result.run().futureValue.toList

        p.paymentMethodType must === (PaymentMethod.CreditCard)
        p.amount must === (None)
      }

      "fails if the order is not found" in new CreditCardFixture {
        val response = POST(s"v1/orders/99/payment-methods/credit-cards/${creditCard.id}")
        response.status must === (StatusCodes.NotFound)
      }

      "fails if the giftCard is not found" in new CreditCardFixture {
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/99")
        response.status must === (StatusCodes.NotFound)
        parseErrors(response).get.head must === (NotFoundFailure(CreditCard, 99).description.head)
      }

      "fails if the creditCard is inActive" in new CreditCardFixture {
        CustomerManager.deleteCreditCard(customerId = customer.id, adminId = admin.id, id = creditCard.id).futureValue
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        response.status must ===(StatusCodes.BadRequest)
        parseErrors(response).get.head must ===(CannotUseInactiveCreditCard(creditCard).description.head)
      }

      "fails if the order is not in cart status" in new CreditCardFixture {
        Orders.findCartByRefNum(order.referenceNumber).map(_.status).update(Order.RemorseHold)
        val first = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        first.status must ===(StatusCodes.OK)

        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        response.status must === (StatusCodes.BadRequest)

        val payments = OrderPayments.findAllCreditCardsForOrder(order.id).result.run().futureValue
        payments must have size(1)
      }

      "fails if the cart already has a credit card" in new CreditCardFixture {
        val first = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        first.status must ===(StatusCodes.OK)

        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        response.status must === (StatusCodes.BadRequest)

        val payments = OrderPayments.findAllCreditCardsForOrder(order.id).result.run().futureValue
        payments must have size(1)
      }
    }
  }

  trait Fixture {
    val (order, admin, customer) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id, status = Order.Cart))
      admin ← StoreAdmins.save(authedStoreAdmin)
    } yield (order, admin, customer)).run().futureValue
  }

  trait AddressFixture extends Fixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id)).run().futureValue
  }

  trait GiftCardFixture extends Fixture {
    val giftCard = (for {
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      giftCard ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
    } yield giftCard).run().futureValue
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      cc ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id, billingAddressId = address.id))
    } yield cc).run().futureValue
  }
}

