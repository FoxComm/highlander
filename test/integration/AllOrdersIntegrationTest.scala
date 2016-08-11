import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.cord.Order._
import models.cord._
import models.customer.Customers
import payloads.OrderPayloads.BulkUpdateOrdersPayload
import responses.BatchResponse
import responses.cord._
import util.Fixtures.EmptyCustomerCartFixture
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class AllOrdersIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "PATCH /v1/orders" - {
    "bulk update states" in new StateUpdateFixture {
      val payload  = BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"), FulfillmentStarted)
      val response = PATCH("v1/orders", payload)

      response.status must === (StatusCodes.OK)

      val all       = response.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must contain allOf (
          ("foo", FulfillmentStarted),
          ("bar", FulfillmentStarted)
      )

      all.errors.value must contain only NotFoundFailure404(Order, "nonExistent").description
    }

    "refuses invalid status transition" in {
      val order = (for {
        customer ← * <~ Customers.create(Factories.customer).gimme
        cart     ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
        order    ← * <~ Orders.create(cart.toOrder()).gimme
      } yield order).gimme

      val response = PATCH("v1/orders", BulkUpdateOrdersPayload(Seq(order.refNum), Shipped))

      response.status must === (StatusCodes.OK)
      val all       = response.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must === (Seq((order.refNum, order.state)))

      all.errors.value.head must === (
          StateTransitionNotAllowed(order.state, Shipped, order.refNum).description)
    }
  }

  trait StateUpdateFixture extends EmptyCustomerCartFixture {
    override def buildCarts =
      Seq("foo", "bar", "baz").map(refNum ⇒
            Factories.cart.copy(customerId = customer.id, referenceNumber = refNum))

    val states = Seq(FraudHold, Order.RemorseHold, ManualHold)
    Orders
      .createAll(carts.zip(states).map { case (cart, state) ⇒ cart.toOrder().copy(state = state) })
      .gimme
  }
}
