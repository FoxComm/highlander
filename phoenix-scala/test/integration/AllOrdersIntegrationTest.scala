import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.cord.Order._
import models.cord._
import models.customer.Customers
import payloads.OrderPayloads.BulkUpdateOrdersPayload
import responses.BatchResponse
import responses.cord._
import util._
import util.apis.PhoenixAdminApi
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class AllOrdersIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "PATCH /v1/orders" - {
    "bulk update states" in new StateUpdateFixture {
      val payload = BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"), FulfillmentStarted)

      val all = ordersApi.update(payload).as[BatchResponse[AllOrders.Root]]

      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))
      allOrders must contain allOf (
          ("foo", FulfillmentStarted),
          ("bar", FulfillmentStarted)
      )
      all.errors.value must contain only NotFoundFailure404(Order, "nonExistent").description
    }

    "refuses invalid status transition" in new Order_Baked {
      val all = ordersApi
        .update(BulkUpdateOrdersPayload(Seq(order.refNum), Shipped))
        .as[BatchResponse[AllOrders.Root]]

      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))
      allOrders must === (Seq((order.refNum, order.state)))

      all.errors.value.head must === (
          StateTransitionNotAllowed(order.state, Shipped, order.refNum).description)
    }
  }

  trait StateUpdateFixture {
    (for {
      cust ← * <~ Customers.create(Factories.customer)
      c = Factories.cart.copy(customerId = cust.id)
      cart  ← * <~ Carts.create(c.copy(referenceNumber = "foo"))
      order ← * <~ Orders.createFromCart(cart)
      _     ← * <~ Orders.update(order, order.copy(state = FraudHold))
      cart  ← * <~ Carts.create(c.copy(referenceNumber = "bar"))
      _     ← * <~ Orders.createFromCart(cart)
      cart  ← * <~ Carts.create(c.copy(referenceNumber = "baz"))
      order ← * <~ Orders.createFromCart(cart)
      _     ← * <~ Orders.update(order, order.copy(state = ManualHold))
    } yield {}).gimme
  }
}
