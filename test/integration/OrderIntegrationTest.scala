import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.cord.Order._
import models.cord.{Order, Orders}
import payloads.OrderPayloads.UpdateOrderPayload
import responses.order.FullOrder
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time._

class OrderIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "PATCH /v1/orders/:refNum" - {

    "successfully" in {
      val order = Orders.create(Factories.order).gimme

      val response = PATCH(s"v1/orders/${order.referenceNumber}", UpdateOrderPayload(FraudHold))

      response.status must === (StatusCodes.OK)

      val responseOrder = response.as[FullOrder.Root]
      responseOrder.orderState must === (FraudHold)
    }

    "fails if transition to destination status is not allowed" in {
      val order = Orders.create(Factories.order).gimme

      val response = PATCH(s"v1/orders/${order.referenceNumber}", UpdateOrderPayload(Shipped))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(order.state, Shipped, order.refNum).description)
    }

    "fails if transition from current status is not allowed" in {
      val order = Orders.create(Factories.order.copy(state = Canceled)).gimme

      val response = PATCH(s"v1/orders/${order.referenceNumber}", UpdateOrderPayload(ManualHold))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(order.state, ManualHold, order.refNum).description)
    }

    "fails if the order is not found" in {
      Orders.create(Factories.order).gimme

      val response = PATCH(s"v1/orders/NOPE", UpdateOrderPayload(ManualHold))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    /* This test should really test against an order and not a *cart*. Karin has filed a story to come back to this
    "cancels order with line items and payments" in new PaymentMethodsFixture {
      (for {
        creditCard ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id, billingAddressId = address
        .id))
        payment ← OrderPayments.save(Factories.orderPayment.copy(orderId = order.id, paymentMethodId = creditCard.id))
        _ ← OrderLineItems ++= Factories.orderLineItems.map(li ⇒ li.copy(orderId = order.id))
      } yield (creditCard, payment)).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(Canceled))

      val responseOrder = parse(response.bodyText).extract[FullOrder.Root]
      responseOrder.orderState must === (Canceled)
      responseOrder.lineItems.head.state must === (OrderLineItem.Canceled)

      // Testing via DB as currently FullOrder returns 'order.state' as 'payment.state'
      // OrderPayments.findAllByOrderId(order.refNum).futureValue.head.state must === ("cancelAuth")
    }
   */
  }

  "POST /v1/orders/:refNum/increase-remorse-period" - {

    "successfully" in {
      val order    = Orders.create(Factories.order.copy(state = Order.RemorseHold)).gimme
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")

      val result = response.as[FullOrder.Root]
      result.remorsePeriodEnd must === (order.remorsePeriodEnd.map(_.plusMinutes(15)))
    }

    "only when in RemorseHold status" in {
      val order    = Orders.create(Factories.order).gimme
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")
      response.status must === (StatusCodes.BadRequest)

      val newOrder = Orders.findByRefNum(order.refNum).one.run().futureValue.value
      newOrder.remorsePeriodEnd must === (order.remorsePeriodEnd)
    }
  }

}
