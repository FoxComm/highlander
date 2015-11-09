import java.time.Instant
import akka.http.scaladsl.model.StatusCodes

import cats.data.Xor
import models.Order._
import models._
import payloads.{BulkAssignment, BulkUpdateOrdersPayload}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{StoreAdminResponse, FullOrder, AllOrders}
import services.orders.OrderQueries
import services.{StatusTransitionNotAllowed, LockedFailure, NotFoundFailure404}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import utils.time._
import cats.implicits._

class AllOrdersIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[AllOrders.Root]
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import api._

  // paging and sorting API
  def uriPrefix = "v1/orders"

  def responseItems = {
    val items = (1 to numOfResults).map { i ⇒
      val dbio = for {
        customer ← Customers.saveNew(Factories.generateCustomer)
        order    ← Orders.saveNew(Factories.order.copy(
          customerId = customer.id,
          referenceNumber = Factories.randomString(10),
          status = Order.RemorseHold,
          remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
      } yield (customer, order)

      dbio.flatMap { case (customer, order) ⇒
        AllOrders.build(order, customer, None)
      }
    }

    DBIO.sequence(items).transactionally.run().futureValue
  }
  val sortColumnName = "referenceNumber"

  def responseItemsSort(items: IndexedSeq[AllOrders.Root]) = items.sortBy(_.referenceNumber)

  def mf = implicitly[scala.reflect.Manifest[AllOrders.Root]]
  // paging and sorting API end

  def getAllOrders: Seq[AllOrders.Root] = {
    OrderQueries.findAll.result.run().futureValue match {
      case Xor.Left(s)    ⇒ fail(s.toList.mkString(";"))
      case Xor.Right(seq) ⇒ seq
    }
  }
  
  "GET /v1/orders" - {
    "find all" in {
      val cId = Customers.saveNew(Factories.customer).run().futureValue.id
      Orders.saveNew(Factories.order.copy(customerId = cId)).run().futureValue

      val responseJson = GET(s"v1/orders")
      responseJson.status must === (StatusCodes.OK)

      val allOrders = responseJson.as[AllOrders.Root#ResponseMetadataSeq].result
      allOrders.size must === (1)

      val actual = allOrders.head

      val expected = AllOrders.Root(
        referenceNumber = "ABCD1234-11",
        email = "yax@yax.com",
        orderStatus = Order.ManualHold,
        placedAt = None,
        total = None,
        paymentStatus = None,
        remorsePeriodEnd = None)

      actual must === (expected)
    }
  }

  "PATCH /v1/orders" - {
    "bulk update statuses" in new StatusUpdateFixture {
      val response = PATCH("v1/orders", BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"), FulfillmentStarted))

      response.status must === (StatusCodes.OK)

      val all = response.as[BulkOrderUpdateResponse]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderStatus))

      allOrders must contain allOf(
        ("foo", FulfillmentStarted),
        ("bar", RemorseHold),
        ("baz", ManualHold))

      all.errors.value must contain allOf(
        LockedFailure(Order, "bar").description.head,
        NotFoundFailure404(Order, "nonExistent").description.head)
    }

    "refuses invalid status transition" in {
      val customer = Customers.saveNew(Factories.customer).run().futureValue
      val order = Orders.saveNew(Factories.order.copy(customerId = customer.id)).run().futureValue
      val response = PATCH("v1/orders", BulkUpdateOrdersPayload(Seq(order.refNum), Cart))

      response.status must === (StatusCodes.OK)
      val all = response.as[BulkOrderUpdateResponse]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderStatus))

      allOrders must === (Seq((order.refNum, order.status)))

      all.errors.value must === (StatusTransitionNotAllowed(order.status, Cart, order.refNum).description)
    }

    "bulk update statuses with paging and sorting" in new StatusUpdateFixture {
      val responseJson = PATCH(
        "v1/orders?size=2&from=2&sortBy=referenceNumber",
        BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"), FulfillmentStarted)
      )

      responseJson.status must === (StatusCodes.OK)

      val all = responseJson.as[BulkOrderUpdateResponse]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderStatus))

      allOrders must contain theSameElementsInOrderAs Seq(
        ("foo", FulfillmentStarted))

      all.errors.value must contain allOf(
        LockedFailure(Order, "bar").description.head,
        NotFoundFailure404(Order, "nonExistent").description.head)
    }
  }

  "POST /v1/orders/assignees" - {
    "assigns successfully ignoring duplicates" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BulkOrderUpdateResponse]
      responseObj1.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj1.errors mustBe empty

      val updOrderResponse1 = GET(s"v1/orders/$orderRef1")
      val updOrder1 = updOrderResponse1.as[FullOrder.Root]
      updOrder1.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))

      // Don't complain about duplicates
      val assignResponse2 = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))
      assignResponse2.status must === (StatusCodes.OK)
      val responseObj2 = assignResponse2.as[BulkOrderUpdateResponse]
      responseObj2.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj2.errors mustBe empty

      val updOrderResponse2 = GET(s"v1/orders/$orderRef1")
      val updOrder2 = updOrderResponse2.as[FullOrder.Root]
      updOrder2.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))

      val updOrderResponse3 = GET(s"v1/orders/$orderRef2")
      val updOrder3 = updOrderResponse3.as[FullOrder.Root]
      updOrder3.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
    }

    "happens successfully ignoring duplicates with sorting and paging" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/orders/assignees?size=1&from=1&sortBy=referenceNumber",
        BulkAssignment(Seq(orderRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BulkOrderUpdateResponse]
      responseObj1.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj1.errors mustBe empty
    }

    "warns when order to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkOrderUpdateResponse]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "warns when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1), 777))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkOrderUpdateResponse]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/orders/assignees/delete" - {
    "unassigns successfully ignoring wrong attempts" in new BulkAssignmentFixture {
      // Should pass
      val unassign1 = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), adminId))
      unassign1.status must === (StatusCodes.OK)

      POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))

      val unassign2 = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), adminId))
      unassign2.status must === (StatusCodes.OK)

      val updOrder1 = GET(s"v1/orders/$orderRef1").as[FullOrder.Root]
      updOrder1.assignees mustBe empty

      val updOrder2 = GET(s"v1/orders/$orderRef2").as[FullOrder.Root]
      updOrder2.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
    }

    "unassigns successfully ignoring wrong attempts with sorting and paging" in new BulkAssignmentFixture {
      POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))

      val unassign = POST(s"v1/orders/assignees/delete?size=1&from=1&sortBy=referenceNumber",
        BulkAssignment(Seq(orderRef1), adminId))
      unassign.status must === (StatusCodes.OK)

      val responseObj = unassign.as[BulkOrderUpdateResponse]
      responseObj.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj.errors mustBe empty
    }

    "warns when order to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkOrderUpdateResponse]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "warns when admin to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), 777))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkOrderUpdateResponse]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  trait StatusUpdateFixture {
    (for {
      customer ← Customers.saveNew(Factories.customer)
      foo ← Orders.saveNew(Factories.order.copy(customerId = customer.id, referenceNumber = "foo", status = FraudHold))
      bar ← Orders.saveNew(Factories.order.copy(customerId = customer.id, referenceNumber = "bar", status = RemorseHold,
        locked = true))
      baz ← Orders.saveNew(Factories.order.copy(customerId = customer.id, referenceNumber = "baz", status = ManualHold))
    } yield (customer, foo, bar)).run().futureValue
  }

  trait BulkAssignmentFixture {
    val (order1, order2, admin) = (for {
      customer ← Customers.saveNew(Factories.customer)
      order1 ← Orders.saveNew(Factories.order.copy(id = 1, referenceNumber = "foo", customerId = customer.id))
      order2 ← Orders.saveNew(Factories.order.copy(id = 2, referenceNumber = "bar", customerId = customer.id))
      admin ← StoreAdmins.saveNew(Factories.storeAdmin)
    } yield (order1, order2, admin)).run().futureValue
    val orderRef1 = order1.referenceNumber
    val orderRef2 = order2.referenceNumber
    val adminId = admin.id
  }
}
