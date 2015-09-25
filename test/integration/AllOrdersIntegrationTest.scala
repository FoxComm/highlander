import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models.Order._
import models._
import org.json4s.jackson.JsonMethods._
import payloads.{BulkAssignment, BulkUpdateOrdersPayload}
import responses.{BulkAssignmentResponse, StoreAdminResponse, FullOrder, BulkOrderUpdateResponse, AllOrders}
import services.{NotFoundFailure, OrderNotFoundFailure, OrderUpdateFailure}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

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
        remorsePeriodEnd = None)

      actual must === (expected)
    }

    "bulk update statuses" in new StatusUpdateFixture {
      val responseJson = PATCH(
        "v1/orders",
          BulkUpdateOrdersPayload(Seq("foo", "bar", "qux", "nonExistent"), FulfillmentStarted)
      )

      responseJson.status must === (StatusCodes.OK)

      val all = parse(responseJson.bodyText).extract[BulkOrderUpdateResponse]
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

  "Bulk assignment" - {

    "happens successfully ignoring duplicates" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1), adminId))
      assignResponse1.status mustBe StatusCodes.OK
      val responseObj1 = parse(assignResponse1.bodyText).extract[BulkOrderUpdateResponse]
      responseObj1.orders.map(_.referenceNumber) mustBe Seq("foo", "bar")
      responseObj1.failures mustBe empty

      val updOrderResponse1 = GET(s"v1/orders/$orderRef1")
      val updOrder1 = parse(updOrderResponse1.bodyText).extract[FullOrder.Root]
      updOrder1.assignees mustBe Seq(StoreAdminResponse.build(admin))

      // Don't complain about duplicates
      val assignResponse2 = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))
      assignResponse2.status mustBe StatusCodes.OK
      val responseObj2 = parse(assignResponse2.bodyText).extract[BulkOrderUpdateResponse]
      responseObj2.orders.map(_.referenceNumber) mustBe Seq("foo", "bar")
      responseObj2.failures mustBe empty

      val updOrderResponse2 = GET(s"v1/orders/$orderRef1")
      val updOrder2 = parse(updOrderResponse2.bodyText).extract[FullOrder.Root]
      updOrder2.assignees mustBe Seq(StoreAdminResponse.build(admin))

      val updOrderResponse3 = GET(s"v1/orders/$orderRef2")
      val updOrder3 = parse(updOrderResponse3.bodyText).extract[FullOrder.Root]
      updOrder3.assignees mustBe Seq(StoreAdminResponse.build(admin))
    }

    "warns when order to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, "NOPE"), adminId))
      response.status mustBe StatusCodes.OK
      val responseObj = parse(response.bodyText).extract[BulkAssignmentResponse]
      responseObj.orders mustBe AllOrders.runFindAll.futureValue
      responseObj.ordersNotFound mustBe Seq(OrderNotFoundFailure("NOPE"))
      responseObj.adminNotFound mustBe None
    }

    "warns when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1), 777))
      response.status mustBe StatusCodes.OK
      val responseObj = parse(response.bodyText).extract[BulkAssignmentResponse]
      responseObj.orders mustBe AllOrders.runFindAll.futureValue
      responseObj.ordersNotFound mustBe empty
      responseObj.adminNotFound mustBe Some(NotFoundFailure(StoreAdmin, 777))
    }

    "unassigns successfully ignoring wrong attempts" in new BulkAssignmentFixture {
      // Should pass
      val unassign1 = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), adminId))
      unassign1.status mustBe StatusCodes.OK

      POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))

      val unassign2 = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), adminId))
      unassign2.status mustBe StatusCodes.OK

      val updOrder1 = parse(GET(s"v1/orders/$orderRef1").bodyText).extract[FullOrder.Root]
      updOrder1.assignees mustBe empty

      val updOrder2 = parse(GET(s"v1/orders/$orderRef2").bodyText).extract[FullOrder.Root]
      updOrder2.assignees mustBe Seq(StoreAdminResponse.build(admin))
    }

    "warns when order to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1, "NOPE"), adminId))
      response.status mustBe StatusCodes.OK
      val responseObj = parse(response.bodyText).extract[BulkAssignmentResponse]
      responseObj.orders mustBe AllOrders.runFindAll.futureValue
      responseObj.ordersNotFound mustBe Seq(OrderNotFoundFailure("NOPE"))
      responseObj.adminNotFound mustBe None
    }

    "warns when admin to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), 777))
      response.status mustBe StatusCodes.OK
      val responseObj = parse(response.bodyText).extract[BulkAssignmentResponse]
      responseObj.orders mustBe AllOrders.runFindAll.futureValue
      responseObj.ordersNotFound mustBe empty
      responseObj.adminNotFound mustBe Some(NotFoundFailure(StoreAdmin, 777))
    }
  }

  trait StatusUpdateFixture {
    (for {
      customer ← Customers.save(Factories.customer)
      foo ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "foo", status = FraudHold))
      bar ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "bar", status = RemorseHold,
        locked = true))
      baz ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "baz", status = ManualHold))
    } yield (customer, foo, bar)).run().futureValue
  }

  trait BulkAssignmentFixture {
    val (order1, order2, admin) = (for {
      customer ← Customers.save(Factories.customer)
      order1 ← Orders.save(Factories.order.copy(id = 1, referenceNumber = "foo", customerId = customer.id))
      order2 ← Orders.save(Factories.order.copy(id = 2, referenceNumber = "bar", customerId = customer.id))
      admin ← StoreAdmins.save(Factories.storeAdmin)
    } yield (order1, order2, admin)).run().futureValue
    val orderRef1 = order1.referenceNumber
    val orderRef2 = order2.referenceNumber
    val adminId = admin.id
  }
}
