import akka.http.scaladsl.model.StatusCodes

import failures.NotFoundFailure404
import models.cord.OrderPayments
import models.payment.creditcard.CreditCards
import models.returns._
import payloads.ReturnPayloads._
import responses.ReturnResponse.Root
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class ReturnPaymentsIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "gift cards" - {
    pending

    "POST /v1/returns/:ref/payment-methods/gift-cards" - {
      "successfully creates gift card as payment method" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 10)
        val response =
          POST(s"v1/returns/${rma.referenceNumber}/payment-methods/gift-cards", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[Root]
        root.payments must have size 1
        root.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload = ReturnPaymentPayload(amount = -10)
        val response =
          POST(s"v1/returns/${rma.referenceNumber}/payment-methods/gift-cards", payload)
        response.status must === (StatusCodes.BadRequest)
        response.error must === ("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = 10)
        val response = POST(s"v1/returns/99/payment-methods/gift-cards", payload)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, 99).description)
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/gift-cards" - {
      "successfully deletes a giftCard" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 10)
        val create  = POST(s"v1/returns/${rma.referenceNumber}/payment-methods/gift-cards", payload)
        create.status must === (StatusCodes.OK)

        val response = DELETE(s"v1/returns/${rma.referenceNumber}/payment-methods/gift-cards")
        response.status must === (StatusCodes.OK)

        val root = response.as[Root]
        root.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = DELETE(s"v1/returns/99/payment-methods/gift-cards")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, 99).description)
      }
    }
  }

  "store credit" - {
    pending

    "POST /v1/returns/:ref/payment-methods/store-credit" - {
      "successfully creates store credit as payment method" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = 75)
        val response = POST(s"v1/returns/${rma.refNum}/payment-methods/store-credit", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[Root]
        root.payments must have size 1
        root.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload = ReturnPaymentPayload(amount = -10)
        val response =
          POST(s"v1/returns/${rma.referenceNumber}/payment-methods/store-credit", payload)
        response.status must === (StatusCodes.BadRequest)
        response.error must === ("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val notFound = rma.copy(referenceNumber = "ABC123")
        val payload  = ReturnPaymentPayload(amount = 50)

        val response = POST(s"v1/returns/${notFound.refNum}/payment-methods/store-credit", payload)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, notFound.refNum).description)
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/store-credit" - {
      "successfully deletes store credit payment" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 75)
        val create  = POST(s"v1/returns/${rma.refNum}/payment-methods/store-credit", payload)
        create.status must === (StatusCodes.OK)

        val response = DELETE(s"v1/returns/${rma.referenceNumber}/payment-methods/store-credit")
        response.status must === (StatusCodes.OK)

        val root = response.as[Root]
        root.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = DELETE(s"v1/returns/99/payment-methods/store-credit")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, 99).description)
      }
    }
  }

  "credit cards" - {
    pending

    "POST /v1/returns/:ref/payment-methods/credit-cards" - {
      "succeeds" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 50)
        val response =
          POST(s"v1/returns/${rma.referenceNumber}/payment-methods/credit-cards", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[Root]
        root.payments must have size 1
        root.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload = ReturnCcPaymentPayload(amount = -10)
        val response =
          POST(s"v1/returns/${rma.referenceNumber}/payment-methods/credit-cards", payload)
        response.status must === (StatusCodes.BadRequest)
        response.error must === ("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val payload  = ReturnCcPaymentPayload(amount = 50)
        val response = POST(s"v1/returns/99/payment-methods/credit-cards", payload)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, 99).description)
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/credit-cards" - {
      "successfully deletes an existing card" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 50)
        val create =
          POST(s"v1/returns/${rma.referenceNumber}/payment-methods/credit-cards", payload)
        create.status must === (StatusCodes.OK)

        val response = DELETE(s"v1/returns/${rma.referenceNumber}/payment-methods/credit-cards")
        response.status must === (StatusCodes.OK)

        val root = response.as[Root]
        root.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = DELETE(s"v1/returns/99/payment-methods/credit-cards")

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, 99).description)
      }
    }
  }

  trait Fixture extends Order_Baked {
    val rma = (for {
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId))
      orderPayment ← * <~ OrderPayments.create(
        Factories.orderPayment
          .copy(cordRef = order.refNum, paymentMethodId = cc.id, amount = None))
      rma ← * <~ Returns.create(Factories.rma.copy(referenceNumber = "ABCD1234-11.1"))
    } yield rma).gimme
  }
}
