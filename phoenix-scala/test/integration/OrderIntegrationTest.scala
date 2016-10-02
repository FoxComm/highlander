import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.cord.Order._
import models.cord._
import models.shipping.ShippingMethods
import payloads.OrderPayloads.UpdateOrderPayload
import responses.cord.OrderResponse
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time._

class OrderIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestObjectContext
    with BakedFixtures {

  "PATCH /v1/orders/:refNum" - {

    "successfully" in new Fixture {
      val response = ordersApi(order.refNum).update(UpdateOrderPayload(FraudHold))
      response.status must === (StatusCodes.OK)
      val responseOrder = response.as[OrderResponse]
      responseOrder.orderState must === (FraudHold)
    }

    "fails if transition to destination status is not allowed" in new Fixture {
      val response = ordersApi(order.refNum).update(UpdateOrderPayload(Shipped))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(order.state, Shipped, order.refNum).description)
    }

    "fails if transition from current status is not allowed" in new EmptyCustomerCart_Baked {
      val order = (for {
        order ← * <~ Orders.createFromCart(cart)
        order ← * <~ Orders.update(order, order.copy(state = Canceled))
      } yield order).gimme

      val response = ordersApi(order.refNum).update(UpdateOrderPayload(ManualHold))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(Canceled, ManualHold, order.refNum).description)
    }

    "fails if the order is not found" in {
      val response = ordersApi("NOPE").update(UpdateOrderPayload(ManualHold))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }
  }

  "POST /v1/orders/:refNum/increase-remorse-period" - {
    "successfully" in new Fixture {
      val response = ordersApi(order.refNum).increaseRemorsePeriod()
      response.status must === (StatusCodes.OK)
      val result = response.as[OrderResponse]
      result.remorsePeriodEnd.value must === (order.remorsePeriodEnd.value.plusMinutes(15))
    }

    "only when in RemorseHold status" in new Fixture {
      Orders.update(order, order.copy(state = FraudHold)).gimme
      val response = ordersApi(order.refNum).increaseRemorsePeriod()
      response.status must === (StatusCodes.BadRequest)
      response.error must === ("Order is not in RemorseHold state")

      val newOrder = Orders.mustFindByRefNum(order.refNum).gimme
      newOrder.state must === (FraudHold)
      newOrder.remorsePeriodEnd must not be defined
    }
  }

  trait Fixture extends EmptyCartWithShipAddress_Baked {
    val order = (for {
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      _          ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(cart.refNum, shipMethod))
      order      ← * <~ Orders.createFromCart(cart)
    } yield order).gimme
  }
}
