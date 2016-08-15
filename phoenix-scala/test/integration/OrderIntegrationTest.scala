import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.cord.Order._
import models.cord._
import models.shipping.ShippingMethods
import payloads.OrderPayloads.UpdateOrderPayload
import responses.cord.OrderResponse
import util._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time._

class OrderIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestObjectContext
    with Fixtures {

  "PATCH /v1/orders/:refNum" - {

    "successfully" in new Fixture {
      val response = PATCH(s"v1/orders/${order.refNum}", UpdateOrderPayload(FraudHold))
      response.status must === (StatusCodes.OK)
      val responseOrder = response.as[OrderResponse]
      responseOrder.orderState must === (FraudHold)
    }

    "fails if transition to destination status is not allowed" in new Fixture {
      val response = PATCH(s"v1/orders/${order.refNum}", UpdateOrderPayload(Shipped))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(order.state, Shipped, order.refNum).description)
    }

    "fails if transition from current status is not allowed" in new EmptyCustomerCartFixture {
      val order = (for {
        order ← * <~ Orders.createFromCart(cart)
        order ← * <~ Orders.update(order, order.copy(state = Canceled))
      } yield order).gimme

      val response = PATCH(s"v1/orders/${order.refNum}", UpdateOrderPayload(ManualHold))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(Canceled, ManualHold, order.refNum).description)
    }

    "fails if the order is not found" in {
      val response = PATCH(s"v1/orders/NOPE", UpdateOrderPayload(ManualHold))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }
  }

  "POST /v1/orders/:refNum/increase-remorse-period" - {
    "successfully" in new Fixture {
      val response = POST(s"v1/orders/${order.refNum}/increase-remorse-period")
      response.status must === (StatusCodes.OK)
      val result = response.as[OrderResponse]
      result.remorsePeriodEnd.value must === (order.remorsePeriodEnd.value.plusMinutes(15))
    }

    "only when in RemorseHold status" in new Fixture {
      Orders.update(order, order.copy(state = FraudHold)).gimme
      val response = POST(s"v1/orders/${order.refNum}/increase-remorse-period")
      response.status must === (StatusCodes.BadRequest)
      response.error must === ("Order is not in RemorseHold state")

      val newOrder = Orders.mustFindByRefNum(order.refNum).gimme
      newOrder.state must === (FraudHold)
      newOrder.remorsePeriodEnd must not be defined
    }
  }

  trait Fixture extends EmptyCustomerCartFixture with AddressFixture {
    val order = (for {
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      _          ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(cart.refNum, shipMethod))
      _          ← * <~ OrderShippingAddresses.copyFromAddress(address = address, cordRef = cart.refNum)
      order      ← * <~ Orders.createFromCart(cart)
    } yield order).gimme
  }
}
