import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.cord.Order._
import models.cord._
import models.shipping.ShippingMethods
import payloads.OrderPayloads.UpdateOrderPayload
import responses.cord.OrderResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
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
      ordersApi(order.refNum)
        .update(UpdateOrderPayload(FraudHold))
        .as[OrderResponse]
        .orderState must === (FraudHold)
    }

    "fails if transition to destination status is not allowed" in new Fixture {
      ordersApi(order.refNum)
        .update(UpdateOrderPayload(Shipped))
        .mustFailWith400(StateTransitionNotAllowed(order.state, Shipped, order.refNum))
    }

    "fails if transition from current status is not allowed" in new EmptyCustomerCart_Baked {
      val order = (for {
        order ← * <~ Orders.createFromCart(cart)
        order ← * <~ Orders.update(order, order.copy(state = Canceled))
      } yield order).gimme

      ordersApi(order.refNum)
        .update(UpdateOrderPayload(ManualHold))
        .mustFailWith400(StateTransitionNotAllowed(Canceled, ManualHold, order.refNum))
    }

    "fails if the order is not found" in {
      ordersApi("NOPE")
        .update(UpdateOrderPayload(ManualHold))
        .mustFailWith404(NotFoundFailure404(Order, "NOPE"))
    }
  }

  "POST /v1/orders/:refNum/increase-remorse-period" - {
    "successfully" in new Fixture {
      val result = ordersApi(order.refNum).increaseRemorsePeriod().as[OrderResponse]
      result.remorsePeriodEnd.value must === (order.remorsePeriodEnd.value.plusMinutes(15))
    }

    "only when in RemorseHold status" in new Fixture {
      Orders.update(order, order.copy(state = FraudHold)).gimme
      ordersApi(order.refNum)
        .increaseRemorsePeriod()
        .mustFailWithMessage("Order is not in RemorseHold state")

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
