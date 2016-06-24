import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.LockFailures.LockedFailure
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.customer.Customers
import models.order.Order._
import models.order.{Order, Orders}
import payloads.OrderPayloads.BulkUpdateOrdersPayload
import responses.BatchResponse
import responses.order._
import util.IntegrationTestBase
import utils.db.DbResultT._
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
          ("bar", RemorseHold)
      )

      all.errors.value must contain allOf (LockedFailure(Order, "bar").description,
          NotFoundFailure404(Order, "nonExistent").description)
    }

    "refuses invalid status transition" in {
      val customer = Customers.create(Factories.customer).gimme
      val order    = Orders.create(Factories.order.copy(customerId = customer.id)).gimme
      val response = PATCH("v1/orders", BulkUpdateOrdersPayload(Seq(order.refNum), Cart))

      response.status must === (StatusCodes.OK)
      val all       = response.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must === (Seq((order.refNum, order.state)))

      all.errors.value.head must === (
          StateTransitionNotAllowed(order.state, Cart, order.refNum).description)
    }
  }

  trait StateUpdateFixture {
    (for {
      cust ← * <~ Customers.create(Factories.customer)
      foo ← * <~ Orders.create(
               Factories.order
                 .copy(customerId = cust.id, referenceNumber = "foo", state = FraudHold))
      bar ← * <~ Orders.create(
               Factories.order.copy(customerId = cust.id,
                                    referenceNumber = "bar",
                                    state = RemorseHold,
                                    isLocked = true))
      baz ← * <~ Orders.create(
               Factories.order
                 .copy(customerId = cust.id, referenceNumber = "baz", state = ManualHold))
    } yield (cust, foo, bar)).gimme
  }
}
