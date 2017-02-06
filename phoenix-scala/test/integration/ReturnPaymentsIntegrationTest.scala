import failures.NotFoundFailure404
import models.cord.OrderPayments
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCards
import models.returns._
import org.scalatest.prop.PropertyChecks
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
    with BakedFixtures
    with PropertyChecks {

  "payment methods" - {
    "POST /v1/returns/:ref/payment-methods" - {
      "succeeds for any supported payment" in new Fixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload = ReturnPaymentPayload(amount = 42, paymentType)
          val response = returnsApi(freshRma.referenceNumber).paymentMethods
            .add(payload)
            .as[ReturnResponse.Root]

          response.payments must have size 1
          response.payments.head.amount must === (payload.amount)
        }
      }

      "fails if the amount is less than zero" in new Fixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload = ReturnPaymentPayload(amount = -42, paymentType)

          val response = returnsApi(freshRma.referenceNumber).paymentMethods.add(payload)
          response.mustFailWithMessage("Amount got -42, expected more than 0")
        }
      }

      "fails if the RMA is not found" in new Fixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload  = ReturnPaymentPayload(amount = 42, paymentType)
          val response = returnsApi("TRY_HARDER").paymentMethods.add(payload)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/credit-cards" - {
      "successfully delete any supported payment method" in new Fixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val response = returnsApi(freshRma.referenceNumber).paymentMethods
            .remove(paymentType)
            .as[ReturnResponse.Root]

          response.payments mustBe 'empty
        }
      }

      "fails if the RMA is not found" in new Fixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val response = returnsApi("TRY_HARDER").paymentMethods.remove(paymentType)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }
    }
  }

  trait Fixture extends Order_Baked {
    val paymentMethodTable = Table("paymentMethod",
                                   PaymentMethod.CreditCard,
                                   PaymentMethod.GiftCard,
                                   PaymentMethod.StoreCredit)
    val cc = CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId)).gimme

    def freshRma =
      (for {
        orderPayment ← * <~ OrderPayments.create(
                          Factories.orderPayment
                            .copy(cordRef = order.refNum, paymentMethodId = cc.id, amount = None))
        rma ← * <~ Returns.create(Factories.rma.copy(orderRef = order.refNum))
      } yield rma).gimme

    lazy val rma = freshRma
  }
}
