import akka.http.scaladsl.model.StatusCodes

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.Order._
import models._
import org.joda.time.DateTime
import services.{OrderPaymentNotFoundFailure, CannotUseInactiveCreditCard, CustomerHasInsufficientStoreCredit,
CustomerManager, GiftCardIsInactive, GiftCardNotEnoughBalance, GiftCardNotFoundFailure, NotFoundFailure,
OrderNotFoundFailure}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._

class OrderPaymentsIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  "gift cards" - {
    "POST /v1/orders/:ref/payment-methods/gift-cards" - {
      "succeeds" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must ===(StatusCodes.NoContent)
        val (p :: Nil) = OrderPayments.findAllByOrderId(order.id).result.run().futureValue.toList

        val payments = giftCardPayments(order)
        payments must have size(1)
        payments.head.amount must === ((Some(payload.amount)))
      }

      "fails if the order is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        giftCardPayments(order) must have size(0)
      }

      "fails if the giftCard is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (GiftCardNotFoundFailure(payload.code).description)
        giftCardPayments(order) must have size(0)
      }

      "fails if the giftCard does not have sufficient available balance" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance + 1)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        parseErrors(response) must === (GiftCardNotEnoughBalance(giftCard, payload.amount).description)
        giftCardPayments(order) must have size(0)
      }

      "fails if the order is not in cart status" in new GiftCardFixture {
        Orders.findCartByRefNum(order.referenceNumber).map(_.status).update(Order.RemorseHold).run().futureValue
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        giftCardPayments(order) must have size(0)
      }

      "fails if the giftCard is inactive" in new GiftCardFixture {
        GiftCards.findByCode(giftCard.code).map(_.status).update(GiftCard.Canceled).run().futureValue
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance + 1)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        parseErrors(response) must === (GiftCardIsInactive(giftCard).description)
        giftCardPayments(order) must have size(0)
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/gift-cards/:code" - {
      "successfully deletes a giftCard" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val create = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)
        create.status must ===(StatusCodes.NoContent)

        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")
        val payments = creditCardPayments(order)

        response.status must ===(StatusCodes.NoContent)
        payments must have size (0)
      }

      "fails if the order is not found" in new GiftCardFixture {
        val response = DELETE(s"v1/orders/99/payment-methods/gift-cards/123")

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (OrderNotFoundFailure("99").description)
        creditCardPayments(order) must have size(0)
      }

      "fails if the giftCard is not found" in new GiftCardFixture {
        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards/abc-123")

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (GiftCardNotFoundFailure("abc-123").description)
        creditCardPayments(order) must have size(0)
      }

      "fails if the giftCard orderPayment is not found" in new GiftCardFixture {
        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (OrderPaymentNotFoundFailure(GiftCard).description)
        creditCardPayments(order) must have size(0)
      }
    }
  }

  "store credit" - {
    "POST /v1/orders/:ref/payment-methods/store-credit" - {
      "when successful" - {
        "uses store credit records in FIFO order according to createdAt" in new StoreCreditFixture {
          // ensure 3 & 4 are oldest so 5th should not be used
          StoreCredits.filter(_.id === 3).map(_.createdAt).update(DateTime.now().minusMonths(2)).run().futureValue
          StoreCredits.filter(_.id === 4).map(_.createdAt).update(DateTime.now().minusMonths(1)).run().futureValue

          val payload = payloads.StoreCreditPayment(amount = 75)
          val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)
          val payments = storeCreditPayments(order)

          response.status must ===(StatusCodes.NoContent)
          payments must have size (2)

          val expected = payments.sortBy(_.paymentMethodId).map(p ⇒ (p.paymentMethodId, p.amount)).toList
          expected must ===(List((3, Some(50)), (4, Some(25))))
        }

        "only uses active store credit" in new StoreCreditFixture {
          // inactive 1 and 2
          StoreCredits.filter(_.id === 1).map(_.status).update(StoreCredit.Canceled).run().futureValue
          StoreCredits.filter(_.id === 2).map(_.availableBalance).update(0).run().futureValue

          val payload = payloads.StoreCreditPayment(amount = 75)
          val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

          response.status must ===(StatusCodes.NoContent)
          val payments = storeCreditPayments(order)
          payments.map(_.paymentMethodId) must contain noneOf(1, 2)
          payments must have size (2)
        }
      }

      "fails if the order is not found" in new Fixture {
        val notFound = order.copy(referenceNumber = "ABC123")
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${notFound.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.NotFound)
        parseErrors(response) must ===(OrderNotFoundFailure(notFound).description)
        storeCreditPayments(order) must have size (0)
      }

      "fails if the customer has no active store credit" in new Fixture {
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.BadRequest)
        val error = CustomerHasInsufficientStoreCredit(customer.id, 0, 50).description
        parseErrors(response) must ===(error)
        storeCreditPayments(order) must have size (0)
      }

      "fails if the customer has insufficient available store credit" in new StoreCreditFixture {
        val payload = payloads.StoreCreditPayment(amount = 251)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.BadRequest)
        val has = storeCredits.map(_.availableBalance).sum
        val error = CustomerHasInsufficientStoreCredit(customer.id, has, payload.amount).description
        parseErrors(response) must ===(error)
        storeCreditPayments(order) must have size (0)
      }

      "fails if the order is not in cart status" in new StoreCreditFixture {
        Orders.findCartByRefNum(order.referenceNumber).map(_.status).update(Order.RemorseHold).run().futureValue
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.NotFound)
        storeCreditPayments(order) must have size (0)
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/store-credit" - {
      "successfully deletes all store credit payments" in new StoreCreditFixture {
        val payload = payloads.StoreCreditPayment(amount = 75)
        val create = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        create.status must ===(StatusCodes.NoContent)

        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/store-credit")

        response.status must ===(StatusCodes.NoContent)
        storeCreditPayments(order) must have size (0)
      }
    }
  }

  "credit cards" - {
    "POST /v1/orders/:ref/payment-methods/credit-cards" - {
      "succeeds" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)
        val payments = creditCardPayments(order)

        response.status must === (StatusCodes.NoContent)
        payments must have size(1)
        payments.head.amount must === (None)
      }

      "fails if the order is not found" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/99/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (OrderNotFoundFailure("99").description)
        creditCardPayments(order) must have size(0)
      }

      "fails if the creditCard is not found" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(99)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (NotFoundFailure(CreditCard, 99).description)
        creditCardPayments(order) must have size(0)
      }

      "fails if the creditCard is inActive" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        CustomerManager.deleteCreditCard(customerId = customer.id, id = creditCard.id).futureValue
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)

        response.status must ===(StatusCodes.BadRequest)
        parseErrors(response) must ===(CannotUseInactiveCreditCard(creditCard).description)
        creditCardPayments(order) must have size(0)
      }

      "fails if the order is not in cart status" in new CreditCardFixture {
        Orders.findCartByRefNum(order.referenceNumber).map(_.status).update(Order.RemorseHold).run().futureValue
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        creditCardPayments(order) must have size(0)
      }
    }

    "PATCH /v1/orders/:ref/payment-methods/credit-cards" - {
      "successfully replaces an existing card" in new CreditCardFixture {
        val first = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards",
          payloads.CreditCardPayment(creditCard.id))
        first.status must ===(StatusCodes.NoContent)

        val newCreditCard = CreditCards.save(creditCard.copy(id = 0, isDefault = false)).run().futureValue
        val second = PATCH(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards",
          payloads.CreditCardPayment(newCreditCard.id))
        second.status must ===(StatusCodes.NoContent)

        val payments = creditCardPayments(order)
        payments must have size (1)
        payments.head.paymentMethodId must === (newCreditCard.id)
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/credit-cards" - {
      "successfully deletes an existing card" in new CreditCardFixture {
        val create = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards",
          payloads.CreditCardPayment(creditCard.id))
        create.status must ===(StatusCodes.NoContent)

        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards")
        val payments = creditCardPayments(order)

        response.status must ===(StatusCodes.NoContent)
        payments must have size (0)
      }

      "fails if the order is not found" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/99/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (OrderNotFoundFailure("99").description)
        creditCardPayments(order) must have size(0)
      }

      "fails if there is no creditCard payment" in new CreditCardFixture {
        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards")
        val payments = creditCardPayments(order)

        response.status must ===(StatusCodes.NotFound)
        payments must have size (0)
      }
    }
  }

  def paymentsFor(order: Order, pmt: PaymentMethod.Type): Seq[OrderPayment] = {
    val q = OrderPayments.byType(pmt).filter(_.orderId === order.id)
    q.result.run().futureValue
  }

  def creditCardPayments(order: Order)  = paymentsFor(order, PaymentMethod.CreditCard)
  def giftCardPayments(order: Order)    = paymentsFor(order, PaymentMethod.GiftCard)
  def storeCreditPayments(order: Order) = paymentsFor(order, PaymentMethod.StoreCredit)

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
      giftCard ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active))
    } yield giftCard).run().futureValue
  }

  trait StoreCreditFixture extends Fixture {
    val storeCredits = (for {
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      _ ← StoreCreditManuals ++= (1 to 5).map { _ ⇒
        Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id)
      }
      _ ← StoreCredits ++= (1 to 5).map { i ⇒
        Factories.storeCredit.copy(status = StoreCredit.Active, customerId = customer.id, originId = i)
      }
      storeCredits ← StoreCredits._findAllByCustomerId(customer.id)
    } yield storeCredits).run().futureValue
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      cc ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id, billingAddressId = address.id))
    } yield cc).run().futureValue
  }
}

