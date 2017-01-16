import cats.syntax.order
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.account._
import models.cord.Order._
import models.cord._
import models.customer._
import payloads.OrderPayloads.BulkUpdateOrdersPayload
import responses.BatchResponse
import responses.cord._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.aliases._
import utils.db._
import utils.seeds.Seeds.Factories

class AllOrdersIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "PATCH /v1/orders" - {
    "bulk update states" in new StoreAdmin_Seed with StateUpdateFixture {
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

  trait StateUpdateFixture extends StoreAdmin_Seed {
    (for {
      acc  ← * <~ Accounts.create(Account())
      cust ← * <~ Users.create(Factories.customer.copy(accountId = acc.id))
      _ ← * <~ CustomersData.create(
             CustomerData(userId = cust.id, accountId = acc.id, scope = Scope.current))
      c = Factories.cart(Scope.current).copy(accountId = acc.id)
      cart  ← * <~ Carts.create(c.copy(referenceNumber = "foo"))
      order ← * <~ Orders.createFromCart(cart, subScope = None)
      _     ← * <~ Orders.update(order, order.copy(state = FraudHold))
      cart  ← * <~ Carts.create(c.copy(referenceNumber = "bar"))
      _     ← * <~ Orders.createFromCart(cart, subScope = None)
      cart  ← * <~ Carts.create(c.copy(referenceNumber = "baz"))
      order ← * <~ Orders.createFromCart(cart, subScope = None)
      _     ← * <~ Orders.update(order, order.copy(state = ManualHold))
    } yield {}).gimme
  }
}
