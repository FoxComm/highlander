import akka.http.scaladsl.model.StatusCodes

import models.customer.Customers
import models.location.Addresses
import models.order.{OrderPayments, Orders}
import models.payment.creditcard.CreditCards
import models.rma.{Rma, Rmas}
import models.StoreAdmins
import responses.RmaResponse.Root
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._
import utils.seeds.Seeds.Factories
import scala.concurrent.ExecutionContext.Implicits.global

import failures.NotFoundFailure404
import payloads.RmaPayloads.{RmaCcPaymentPayload, RmaPaymentPayload}

class RmaPaymentsIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import Extensions._

  "gift cards" - {
    pending

    "POST /v1/rmas/:ref/payment-methods/gift-cards" - {
      "successfully creates gift card as payment method" in new Fixture {
        val payload = RmaPaymentPayload(amount = 10)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/payment-methods/gift-cards", payload)
        response.status must ===(StatusCodes.OK)

        val root = response.as[Root]
        root.payments must have size 1
        root.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload = RmaPaymentPayload(amount = -10)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/payment-methods/gift-cards", payload)
        response.status must ===(StatusCodes.BadRequest)
        response.error must === ("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val payload = RmaPaymentPayload(amount = 10)
        val response = POST(s"v1/rmas/99/payment-methods/gift-cards", payload)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, 99).description)
      }
    }

    "DELETE /v1/rmas/:ref/payment-methods/gift-cards" - {
      "successfully deletes a giftCard" in new Fixture {
        val payload = RmaPaymentPayload(amount = 10)
        val create = POST(s"v1/rmas/${rma.referenceNumber}/payment-methods/gift-cards", payload)
        create.status must ===(StatusCodes.OK)

        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/payment-methods/gift-cards")
        response.status must ===(StatusCodes.OK)

        val root = response.as[Root]
        root.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = DELETE(s"v1/rmas/99/payment-methods/gift-cards")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, 99).description)
      }
    }
  }

  "store credit" - {
    pending

    "POST /v1/rmas/:ref/payment-methods/store-credit" - {
      "successfully creates store credit as payment method" in new Fixture {
        val payload = RmaPaymentPayload(amount = 75)
        val response = POST(s"v1/rmas/${rma.refNum}/payment-methods/store-credit", payload)
        response.status must ===(StatusCodes.OK)

        val root = response.as[Root]
        root.payments must have size 1
        root.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload = RmaPaymentPayload(amount = -10)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/payment-methods/store-credit", payload)
        response.status must ===(StatusCodes.BadRequest)
        response.error must === ("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val notFound = rma.copy(referenceNumber = "ABC123")
        val payload = RmaPaymentPayload(amount = 50)

        val response = POST(s"v1/rmas/${notFound.refNum}/payment-methods/store-credit", payload)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, notFound.refNum).description)
      }
    }

    "DELETE /v1/rmas/:ref/payment-methods/store-credit" - {
      "successfully deletes store credit payment" in new Fixture {
        val payload = RmaPaymentPayload(amount = 75)
        val create = POST(s"v1/rmas/${rma.refNum}/payment-methods/store-credit", payload)
        create.status must ===(StatusCodes.OK)

        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/payment-methods/store-credit")
        response.status must ===(StatusCodes.OK)

        val root = response.as[Root]
        root.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = DELETE(s"v1/rmas/99/payment-methods/store-credit")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, 99).description)
      }
    }
  }

  "credit cards" - {
    pending
    
    "POST /v1/rmas/:ref/payment-methods/credit-cards" - {
      "succeeds" in new Fixture {
        val payload = RmaPaymentPayload(amount = 50)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/payment-methods/credit-cards", payload)
        response.status must ===(StatusCodes.OK)

        val root = response.as[Root]
        root.payments must have size 1
        root.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload = RmaCcPaymentPayload(amount = -10)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/payment-methods/credit-cards", payload)
        response.status must ===(StatusCodes.BadRequest)
        response.error must === ("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val payload = RmaCcPaymentPayload(amount = 50)
        val response = POST(s"v1/rmas/99/payment-methods/credit-cards", payload)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, 99).description)
      }
    }

    "DELETE /v1/rmas/:ref/payment-methods/credit-cards" - {
      "successfully deletes an existing card" in new Fixture {
        val payload = RmaPaymentPayload(amount = 50)
        val create = POST(s"v1/rmas/${rma.referenceNumber}/payment-methods/credit-cards", payload)
        create.status must ===(StatusCodes.OK)

        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/payment-methods/credit-cards")
        response.status must ===(StatusCodes.OK)

        val root = response.as[Root]
        root.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = DELETE(s"v1/rmas/99/payment-methods/credit-cards")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, 99).description)
      }
    }
  }

  trait Fixture {
    val (rma, order, admin, customer) = (for {
      admin ← * <~ StoreAdmins.create(authedStoreAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
      orderPayment ← * <~ OrderPayments.create(Factories.orderPayment.copy(orderId = order.id,
        paymentMethodId = cc.id, amount = None))
      rma ← * <~ Rmas.create(Factories.rma.copy(referenceNumber = "ABCD1234-11.1"))
    } yield (rma, order, admin, customer)).runTxn().futureValue.rightVal
  }
}
