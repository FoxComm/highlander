import failures.NotFoundFailure404
import models.cord.OrderPayments
import models.payment.creditcard.CreditCards
import models.returns._
import payloads.ReturnPayloads._
import responses.ReturnResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class ReturnPaymentsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "credit cards" - {
    "POST /v1/returns/:ref/payment-methods/credit-cards" - {
      "succeeds" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 50)
        val response = returnsApi(rma.referenceNumber).paymentMethods.creditCards
          .add(payload)
          .as[ReturnResponse.Root]

        response.payments must have size 1
        response.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = -10)
        val response = returnsApi(rma.referenceNumber).paymentMethods.creditCards.add(payload)

        response.mustFailWithMessage("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = 50)
        val response = returnsApi("42").paymentMethods.creditCards.add(payload)

        response.mustFailWith404(NotFoundFailure404(Return, 42))
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/credit-cards" - {
      "successfully deletes an existing card" in new Fixture {
        val response = returnsApi(rma.referenceNumber).paymentMethods.creditCards
          .delete()
          .as[ReturnResponse.Root]

        response.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = returnsApi("42").paymentMethods.creditCards.delete()

        response.mustFailWith404(NotFoundFailure404(Return, "42"))
      }
    }
  }

  "gift cards" - {
    "POST /v1/returns/:ref/payment-methods/gift-cards" - {
      "successfully creates gift card as payment method" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 10)
        val response = returnsApi(rma.referenceNumber).paymentMethods.giftCards
          .add(payload)
          .as[ReturnResponse.Root]

        response.payments must have size 1
        response.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = -10)
        val response = returnsApi(rma.referenceNumber).paymentMethods.giftCards.add(payload)

        response.mustFailWithMessage("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = 10)
        val response = returnsApi("42").paymentMethods.giftCards.add(payload)

        response.mustFailWith404(NotFoundFailure404(Return, "42"))
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/gift-cards" - {
      "successfully deletes a giftCard" in new Fixture {
        val response =
          returnsApi(rma.referenceNumber).paymentMethods.giftCards.delete().as[ReturnResponse.Root]

        response.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = returnsApi("42").paymentMethods.giftCards.delete()

        response.mustFailWith404(NotFoundFailure404(Return, "42"))
      }
    }
  }

  "store credit" - {
    "POST /v1/returns/:ref/payment-methods/store-credit" - {
      "successfully creates store credit as payment method" in new Fixture {
        val payload = ReturnPaymentPayload(amount = 75)
        val response = returnsApi(rma.referenceNumber).paymentMethods.storeCredit
          .add(payload)
          .as[ReturnResponse.Root]

        response.payments must have size 1
        response.payments.head.amount must === (payload.amount)
      }

      "fails if the amount is less than zero" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = -10)
        val response = returnsApi(rma.referenceNumber).paymentMethods.storeCredit.add(payload)

        response.mustFailWithMessage("Amount got -10, expected more than 0")
      }

      "fails if the RMA is not found" in new Fixture {
        val payload  = ReturnPaymentPayload(amount = 50)
        val response = returnsApi("42").paymentMethods.storeCredit.add(payload)

        response.mustFailWith404(NotFoundFailure404(Return, "42"))
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/store-credit" - {
      "successfully deletes store credit payment" in new Fixture {
        val response = returnsApi(rma.referenceNumber).paymentMethods.storeCredit
          .delete()
          .as[ReturnResponse.Root]

        response.payments mustBe 'empty
      }

      "fails if the RMA is not found" in new Fixture {
        val response = returnsApi("42").paymentMethods.storeCredit.delete()

        response.mustFailWith404(NotFoundFailure404(Return, "42"))
      }
    }
  }

  trait Fixture extends Order_Baked {
    val rma = (for {
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId))
      orderPayment ← * <~ OrderPayments.create(
                        Factories.orderPayment
                          .copy(cordRef = order.refNum, paymentMethodId = cc.id, amount = None))
      rma ← * <~ Returns.create(Factories.rma.copy(orderRef = order.refNum))
    } yield rma).gimme
  }
}
