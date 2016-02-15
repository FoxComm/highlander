import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes

import models.order._
import Order._
import models.activity.ActivityContext
import models.customer.Customers
import models.location.Addresses
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCards, CreditCard}
import models.payment.giftcard.{GiftCardManuals, GiftCardManual, GiftCards, GiftCard}
import models.payment.storecredit.{StoreCreditManuals, StoreCreditManual, StoreCredits, StoreCredit}
import models.{StoreAdmins, Reasons}
import OrderPayments.scope._
import services.{GiftCardMustNotBeCart, OrderPaymentNotFoundFailure, CannotUseInactiveCreditCard,
CustomerHasInsufficientStoreCredit, CreditCardManager, GiftCardIsInactive, GiftCardNotEnoughBalance, NotFoundFailure404}
import services.CartFailures.OrderMustBeCart
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

import utils.time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

class OrderPaymentsIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import Extensions._

  "gift cards" - {
    "POST /v1/orders/:ref/payment-methods/gift-cards" - {
      "succeeds" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must ===(StatusCodes.OK)
        val (p :: Nil) = OrderPayments.findAllByOrderId(order.id).result.run().futureValue.toList

        val payments = giftCardPayments(order)
        payments must have size(1)
        payments.head.amount must === ((Some(payload.amount)))
      }

      "fails if the order is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        giftCardPayments(order) mustBe 'empty
      }

      "fails if the giftCard is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(GiftCard, payload.code).description)
        giftCardPayments(order) mustBe 'empty
      }

      "fails if the giftCard does not have sufficient available balance" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance + 1)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (GiftCardNotEnoughBalance(giftCard, payload.amount).description)
        giftCardPayments(order) mustBe 'empty
      }

      "FIXME fails if the order is not in cart status" in new GiftCardFixture {
        Orders.findByRefNum(order.referenceNumber).map(_.state).update(Order.RemorseHold).run().futureValue
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderMustBeCart(order.refNum).description)
        giftCardPayments(order) must have size 0
      }

      "fails if the giftCard is inactive" in new GiftCardFixture {
        GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).run().futureValue
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance + 1)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (GiftCardIsInactive(giftCard).description)
        giftCardPayments(order) mustBe 'empty
      }

      "fails to add GC with cart status as payment method" in new GiftCardFixture {
        GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Cart).run().futureValue
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = 15)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/gift-cards", payload)

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GiftCardMustNotBeCart(giftCard.code).description)
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/gift-cards/:code" - {
      "successfully deletes a giftCard" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val create = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)
        create.status must ===(StatusCodes.OK)

        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")
        val payments = creditCardPayments(order)

        response.status must ===(StatusCodes.OK)
        payments mustBe 'empty
      }

      "fails if the order is not found" in new GiftCardFixture {
        val response = DELETE(s"v1/orders/99/payment-methods/gift-cards/123")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Order, 99).description)
        creditCardPayments(order) mustBe 'empty
      }

      "fails if the giftCard is not found" in new GiftCardFixture {
        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards/abc-123")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(GiftCard, "abc-123").description)
        creditCardPayments(order) mustBe 'empty
      }

      "fails if the giftCard orderPayment is not found" in new GiftCardFixture {
        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderPaymentNotFoundFailure(GiftCard).description)
        creditCardPayments(order) mustBe 'empty
      }
    }
  }

  "store credit" - {
    "POST /v1/orders/:ref/payment-methods/store-credit" - {
      "when successful" - {
        "uses store credit records in FIFO order according to createdAt" in new StoreCreditFixture {
          // ensure 3 & 4 are oldest so 5th should not be used
          StoreCredits.filter(_.id === 3).map(_.createdAt).update(ZonedDateTime.now().minusMonths(2).toInstant).run().futureValue
          StoreCredits.filter(_.id === 4).map(_.createdAt).update(ZonedDateTime.now().minusMonths(1).toInstant).run().futureValue

          val payload = payloads.StoreCreditPayment(amount = 7500)
          val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)
          val payments = storeCreditPayments(order)

          response.status must ===(StatusCodes.OK)
          payments must have size (2)

          val expected = payments.sortBy(_.paymentMethodId).map(p ⇒ (p.paymentMethodId, p.amount)).toList
          expected must ===(List((3, Some(5000)), (4, Some(2500))))
        }

        "only uses active store credit" in new StoreCreditFixture {
          // inactive 1 and 2
          StoreCredits.filter(_.id === 1).map(_.state).update(StoreCredit.Canceled).run().futureValue
          StoreCredits.filter(_.id === 2).map(_.availableBalance).update(0).run().futureValue

          val payload = payloads.StoreCreditPayment(amount = 7500)
          val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

          response.status must ===(StatusCodes.OK)
          val payments = storeCreditPayments(order)
          payments.map(_.paymentMethodId) must contain noneOf(1, 2)
          payments must have size (2)
        }

        "adding or editing store credit should remove previous order payments" in new StoreCreditFixture {
          val payload = payloads.StoreCreditPayment(amount = 7500)
          val createdResponse = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)
          val createdPayments = storeCreditPayments(order)

          createdResponse.status must ===(StatusCodes.OK)
          createdPayments must have size (2)

          val createdPaymentIds = createdPayments.map(_.id).toList
          val editedResponse = PATCH(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)
          val editedPayments = storeCreditPayments(order)

          editedResponse.status must ===(StatusCodes.OK)
          editedPayments must have size (2)
          editedPayments.map(_.id) mustNot contain theSameElementsAs(createdPaymentIds)
        }
      }

      "fails if the order is not found" in new Fixture {
        val notFound = order.copy(referenceNumber = "ABC123")
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${notFound.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Order, notFound.refNum).description)
        storeCreditPayments(order) mustBe 'empty
      }

      "fails if the customer has no active store credit" in new Fixture {
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.BadRequest)
        val error = CustomerHasInsufficientStoreCredit(customer.id, 0, 50).description
        response.error must ===(error)
        storeCreditPayments(order) mustBe 'empty
      }

      "fails if the customer has insufficient available store credit" in new StoreCreditFixture {
        val payload = payloads.StoreCreditPayment(amount = 25100)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.BadRequest)
        val has = storeCredits.map(_.availableBalance).sum
        val error = CustomerHasInsufficientStoreCredit(customer.id, has, payload.amount).description
        response.error must ===(error)
        storeCreditPayments(order) mustBe 'empty
      }

      "fails if the order is not in cart status" in new StoreCreditFixture {
        Orders.findCartByRefNum(order.referenceNumber).map(_.state).update(Order.RemorseHold).run().futureValue
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderMustBeCart(order.refNum).description)
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/store-credit" - {
      "successfully deletes all store credit payments" in new StoreCreditFixture {
        val payload = payloads.StoreCreditPayment(amount = 75)
        val create = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        create.status must ===(StatusCodes.OK)

        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/store-credit")

        response.status must ===(StatusCodes.OK)
        storeCreditPayments(order) mustBe 'empty
      }
    }
  }

  "credit cards" - {
    "POST /v1/orders/:ref/payment-methods/credit-cards" - {
      "succeeds" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)
        val payments = creditCardPayments(order)

        response.status must ===(StatusCodes.OK)
        payments must have size(1)
        payments.head.amount must === (None)
      }

      "fails if the order is not found" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/99/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Order, 99).description)
        creditCardPayments(order) mustBe 'empty
      }

      "fails if the creditCard is not found" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(99)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(CreditCard, 99).description)
        creditCardPayments(order) mustBe 'empty
      }

      "fails if the creditCard is inActive" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        implicit val ac = ActivityContext(userId = 1, userType = "b", transactionId = "c")
        CreditCardManager.deleteCreditCard(admin = admin, customerId = customer.id, id = creditCard.id).futureValue
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(CannotUseInactiveCreditCard(creditCard).description)
        creditCardPayments(order) mustBe 'empty
      }

      "fails if the order is not in cart status" in new CreditCardFixture {
        Orders.findCartByRefNum(order.referenceNumber).map(_.state).update(Order.RemorseHold).run().futureValue
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderMustBeCart(order.refNum).description)
        creditCardPayments(order) must have size 0
      }
    }

    "PATCH /v1/orders/:ref/payment-methods/credit-cards" - {
      "successfully replaces an existing card" in new CreditCardFixture {
        val first = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards",
          payloads.CreditCardPayment(creditCard.id))
        first.status must ===(StatusCodes.OK)

        val newCreditCard = CreditCards.create(creditCard.copy(id = 0, isDefault = false)).run().futureValue.rightVal
        val second = PATCH(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards",
          payloads.CreditCardPayment(newCreditCard.id))
        second.status must ===(StatusCodes.OK)

        val payments = creditCardPayments(order)
        payments must have size (1)
        payments.head.paymentMethodId must === (newCreditCard.id)
      }
    }

    "DELETE /v1/orders/:ref/payment-methods/credit-cards" - {
      "successfully deletes an existing card" in new CreditCardFixture {
        val create = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards",
          payloads.CreditCardPayment(creditCard.id))
        create.status must ===(StatusCodes.OK)

        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards")
        val payments = creditCardPayments(order)

        response.status must ===(StatusCodes.OK)
        payments mustBe 'empty
      }

      "fails if the order is not found" in new CreditCardFixture {
        val payload = payloads.CreditCardPayment(creditCard.id)
        val response = POST(s"v1/orders/99/payment-methods/credit-cards", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Order, 99).description)
        creditCardPayments(order) mustBe 'empty
      }

      "fails if there is no creditCard payment" in new CreditCardFixture {
        val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards")
        val payments = creditCardPayments(order)

        response.status must ===(StatusCodes.BadRequest)
        payments mustBe 'empty
      }
    }
  }

  def paymentsFor(order: Order, pmt: PaymentMethod.Type): Seq[OrderPayment] = {
    val q = OrderPayments.filter(_.orderId === order.id).byType(pmt)
    q.result.run().futureValue
  }

  def creditCardPayments(order: Order)  = paymentsFor(order, PaymentMethod.CreditCard)
  def giftCardPayments(order: Order)    = paymentsFor(order, PaymentMethod.GiftCard)
  def storeCreditPayments(order: Order) = paymentsFor(order, PaymentMethod.StoreCredit)

  trait Fixture {
    val (order, admin, customer) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id, state = Order.Cart))
      admin    ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, admin, customer)).runTxn().futureValue.rightVal
  }

  trait GiftCardFixture extends Fixture {
    val giftCard = (for {
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      origin   ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).runTxn().futureValue.rightVal
  }

  trait StoreCreditFixture extends Fixture {
    val storeCredits = (for {
      reason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      _ ← * <~ StoreCreditManuals.createAll((1 to 5).map { _ ⇒
        StoreCreditManual(adminId = admin.id, reasonId = reason.id)
      })
      _ ← * <~ StoreCredits.createAll((1 to 5).map { i ⇒
        Factories.storeCredit.copy(state = StoreCredit.Active, customerId = customer.id, originId = i)
      })
      storeCredits ← * <~ StoreCredits.findAllByCustomerId(customer.id).result
    } yield storeCredits).runTxn().futureValue.rightVal
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = (for {
      address ← Addresses.create(Factories.address.copy(customerId = customer.id))
      cc      ← CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
    } yield cc).run().futureValue.rightVal
  }
}
