import cats.implicits._
import models.payment.PaymentMethod
import models.returns.Return
import models.returns.Return.{Pending, Processing, ReturnType}
import payloads.ReturnPayloads.{ReturnMessageToCustomerPayload, ReturnPaymentPayload, ReturnUpdateStatePayload}
import responses.ReturnResponse
import testutils._
import testutils.fixtures.{BakedFixtures, ReturnsFixtures}

case class ReturnsSearchViewResult(
    id: Int,
    referenceNumber: String,
    orderId: Int,
    orderRef: String,
    createdAt: String,
    state: Return.State,
    totalRefund: Option[Int],
    messageToAccount: Option[String],
    returnType: ReturnType,
    customer: CustomerSearchViewResult
)

case class CustomerSearchViewResult(
    id: Int,
    name: String,
    email: String,
    isBlacklisted: Boolean,
    joinedAt: String,
    revenue: Int
)

class ReturnsSearchViewTest
    extends SearchViewTestBase
    with TestSeeds
    with BakedFixtures
    with ReturnsFixtures {

  type SearchViewResult = ReturnsSearchViewResult
  val searchViewName: String = "returns_search_view"
  val searchKeyName: String  = "id"

  "Returns search view row must be found when" - {
    "a return was created" in new ReturnPaymentDefaults {
      createReturnPayment(Map(PaymentMethod.CreditCard → 100), rma.referenceNumber)

      returnsApi(rma.referenceNumber).paymentMethods
        .add(PaymentMethod.CreditCard, ReturnPaymentPayload(amount = 20))
        .as[ReturnResponse.Root]

      val rmaSearchView = viewOne(rma.id)

      {
        import rmaSearchView._

        id must === (rma.id)
        referenceNumber must === (rma.referenceNumber)
        orderRef must === (rma.cordRefNum)
        state must === (rma.state)
        messageToAccount must === (rma.messageToCustomer)
        returnType must === (rma.rmaType)
        createdAt must === (rma.createdAt.toString)
        totalRefund.nonEmpty must === (true)
        totalRefund must === (Some(120))

        rma.customer.map(c ⇒ {
          rmaSearchView.customer.id must === (c.id)
          rmaSearchView.customer.name.some must === (c.name)
          rmaSearchView.customer.email.some must === (c.email)
        })

      }
    }
  }

  "Returns search view row must be updated when" - {
    "a return state was updated" in new ReturnDefaults {
      assert(rma.state == Pending)
      viewOne(rma.id).state must === (rma.state)

      returnsApi(rma.referenceNumber)
        .update(ReturnUpdateStatePayload(state = Processing))
        .as[ReturnResponse.Root]
        .state must === (Processing)

      viewOne(rma.id).state must === (Processing)
    }

    "a return message-to-customer was updated" in new ReturnDefaults {
      viewOne(rma.id).messageToAccount must === (None)

      val payload = ReturnMessageToCustomerPayload(message = "Hello!")
      returnsApi(rma.referenceNumber)
        .message(payload)
        .as[ReturnResponse.Root]
        .messageToCustomer
        .head must === (payload.message)

      viewOne(rma.id).messageToAccount must === ("Hello!".some)
    }
  }

}
