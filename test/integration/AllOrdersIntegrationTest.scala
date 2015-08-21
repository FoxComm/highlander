import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models.Order._
import models._
import org.json4s.jackson.JsonMethods._
import payloads.BulkUpdateOrdersPayload
import responses.{AllOrdersWithFailures, AllOrders}
import services.OrderUpdateFailure
import util.IntegrationTestBase
import utils.Seeds.Factories

class AllOrdersIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "All orders" - {

    "find all" in {

      val cId = Customers.save(Factories.customer).run().futureValue.id
      Orders.save(Factories.order.copy(customerId = cId)).run().futureValue

      val responseJson = GET(s"v1/orders")
      responseJson.status must === (StatusCodes.OK)

      val allOrders = parse(responseJson.bodyText).extract[Seq[AllOrders.Root]]
      allOrders.size must === (1)

      val actual = allOrders.head

      val expected = responses.AllOrders.Root(
        referenceNumber = "ABCD1234-11",
        email = "yax@yax.com",
        orderStatus = Order.ManualHold,
        placedAt = None,
        total = 27,
        paymentStatus = None,
        remorsePeriod = None)

      actual must === (expected)
    }

    "bulk update statuses" in new Fixture {
      val responseJson = PATCH(
        "v1/orders",
          BulkUpdateOrdersPayload(Seq("foo", "bar", "qux", "nonExistent"), FulfillmentStarted)
      )

      responseJson.status must === (StatusCodes.OK)

      val all = parse(responseJson.bodyText).extract[AllOrdersWithFailures]
      val allOrders = all.orders.map(o ⇒ (o.referenceNumber, o.orderStatus))

      allOrders must contain allOf(
        ("foo", FulfillmentStarted),
        ("bar", RemorseHold),
        ("baz", ManualHold))

      all.failures must contain allOf(
        OrderUpdateFailure("bar", "Order is locked"),
        OrderUpdateFailure("nonExistent", "Not found"))
    }
  }

  trait Fixture {
    val q = for {
      customer ← Customers.save(Factories.customer)
      foo ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "foo", status = FraudHold))
      bar ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "bar", status = RemorseHold,
        locked = true))
      baz ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "baz", status = ManualHold))
    } yield (customer, foo, bar)
    db.run(q).futureValue
  }
}
