import java.time.Instant

import Extensions._
import akka.http.scaladsl.model.StatusCodes
import cats.data.Xor
import models.order.{Orders, Order}
import Order._
import models.customer.Customers
import models.payment.creditcard.CreditCardCharge
import models.{StoreAdmin, StoreAdmins}
import payloads.{BulkAssignmentPayload, BulkUpdateOrdersPayload}
import responses.BatchResponse
import responses.order._
import responses.StoreAdminResponse
import services.orders.OrderQueries
import services.{LockedFailure, NotFoundFailure404, StateTransitionNotAllowed}
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories
import utils.seeds.RankingSeedsGenerator
import utils.time._

import scala.concurrent.ExecutionContext.Implicits.global

class AllOrdersIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[AllOrders.Root]
  with AutomaticAuth {

  // paging and sorting API
  def uriPrefix = "v1/orders"

  def responseItems = {
    val dbio = for {
      customer ← * <~ Customers.create(RankingSeedsGenerator.generateCustomer)
      insertOrders = (1 to numOfResults).map { _ ⇒ Factories.order.copy(
        customerId = customer.id,
        referenceNumber = RankingSeedsGenerator.randomString(10),
        state = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))) }

      _ ← * <~ Orders.createAll(insertOrders)
    } yield ()

    dbio.runTxn().futureValue
    getAllOrders.toIndexedSeq
  }

  val sortColumnName = "referenceNumber"

  def responseItemsSort(items: IndexedSeq[AllOrders.Root]) = items.sortBy(_.referenceNumber)

  def mf = implicitly[scala.reflect.Manifest[AllOrders.Root]]
  // paging and sorting API end

  def getAllOrders: Seq[AllOrders.Root] = {
    OrderQueries.list.futureValue match {
      case Xor.Left(s)    ⇒ fail(s.toList.mkString(";"))
      case Xor.Right(seq) ⇒ seq.result
    }
  }

  "GET /v1/orders" - {
    "find all" in {
      val cId = Customers.create(Factories.customer).run().futureValue.rightVal.id
      Orders.create(Factories.order.copy(customerId = cId)).run().futureValue.rightVal

      val responseJson = GET(s"v1/orders")
      responseJson.status must === (StatusCodes.OK)

      val allOrders = responseJson.ignoreFailuresAndGiveMe[Seq[AllOrders.Root]]
      allOrders.size must === (1)

      val actual = allOrders.head

      val expected = AllOrders.Root(
        referenceNumber = "ABCD1234-11",
        name = Some("Yax Fuentes"),
        email = "yax@yax.com",
        orderState = Order.ManualHold,
        paymentState = CreditCardCharge.Cart,
        shippingState = Some(Order.ManualHold),
        placedAt = None,
        total = 0,
        remorsePeriodEnd = None)

      actual must === (expected)
    }
  }

  "PATCH /v1/orders" - {
    "bulk update states" in new StateUpdateFixture {
      val response = PATCH("v1/orders", BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"), FulfillmentStarted))

      response.status must === (StatusCodes.OK)

      val all = response.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must contain allOf(
        ("foo", FulfillmentStarted),
        ("bar", RemorseHold)
        )

      all.errors.value must contain allOf(
        LockedFailure(Order, "bar").description,
        NotFoundFailure404(Order, "nonExistent").description)
    }

    "refuses invalid status transition" in {
      val customer = Customers.create(Factories.customer).run().futureValue.rightVal
      val order = Orders.create(Factories.order.copy(customerId = customer.id)).run().futureValue.rightVal
      val response = PATCH("v1/orders", BulkUpdateOrdersPayload(Seq(order.refNum), Cart))

      response.status must === (StatusCodes.OK)
      val all = response.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must === (Seq((order.refNum, order.state)))

      all.errors.value.head must === (StateTransitionNotAllowed(order.state, Cart, order.refNum).description)
    }

    "bulk update states with paging and sorting" in new StateUpdateFixture {
      val responseJson = PATCH(
        "v1/orders?size=2&from=1&sortBy=referenceNumber",
        BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"), FulfillmentStarted)
      )

      responseJson.status must === (StatusCodes.OK)

      val all = responseJson.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must contain theSameElementsInOrderAs Seq(
        ("foo", FulfillmentStarted)
      )

      all.errors.value must contain allOf(
        LockedFailure(Order, "bar").description,
        NotFoundFailure404(Order, "nonExistent").description)
    }
  }

  "POST /v1/orders/assignees" - {
    "assigns successfully ignoring duplicates" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/orders/assignees", BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BatchResponse[AllOrders.Root]]
      responseObj1.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj1.errors mustBe empty

      // FIXME
      /*
      val updOrderResponse1 = GET(s"v1/orders/$orderRef1")
      val updOrder1 = updOrderResponse1.withResultTypeOf[FullOrder.Root].result
      updOrder1.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
      */

      // Don't complain about duplicates
      val assignResponse2 = POST(s"v1/orders/assignees", BulkAssignmentPayload[String](Seq(orderRef1, orderRef2), adminId))
      assignResponse2.status must === (StatusCodes.OK)
      val responseObj2 = assignResponse2.as[BatchResponse[AllOrders.Root]]
      responseObj2.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj2.errors mustBe empty

      // FIXME
      /*
      val updOrderResponse2 = GET(s"v1/orders/$orderRef1")
      val updOrder2 = updOrderResponse2.withResultTypeOf[FullOrder.Root].result
      updOrder2.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))

      val updOrderResponse3 = GET(s"v1/orders/$orderRef2")
      val updOrder3 = updOrderResponse3.withResultTypeOf[FullOrder.Root].result
      updOrder3.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
      */
    }

    "happens successfully ignoring duplicates with sorting and paging" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/orders/assignees?size=1&from=1&sortBy=referenceNumber",
        BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BatchResponse[AllOrders.Root]]
      responseObj1.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj1.errors mustBe empty
    }

    "warns when order to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignmentPayload[String](Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BatchResponse[AllOrders.Root]]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/orders/assignees/delete" - {
    "unassigns successfully ignoring wrong attempts" in new BulkAssignmentFixture {
      // Should pass
      val unassign1 = POST(s"v1/orders/assignees/delete", BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      unassign1.status must === (StatusCodes.OK)

      POST(s"v1/orders/assignees", BulkAssignmentPayload[String](Seq(orderRef1, orderRef2), adminId))

      val unassign2 = POST(s"v1/orders/assignees/delete", BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      unassign2.status must === (StatusCodes.OK)

      // FIXME
      /*
      val updOrder1 = GET(s"v1/orders/$orderRef1")
      updOrder1.status must === (StatusCodes.OK)

      val updOrder1Root = updOrder1.withResultTypeOf[FullOrder.Root].result
      updOrder1Root.assignees mustBe empty

      val updOrder2 = GET(s"v1/orders/$orderRef2")
      updOrder2.status must === (StatusCodes.OK)

      val updOrder2Root = updOrder2.withResultTypeOf[FullOrder.Root].result
      updOrder2Root.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
      */
    }

    "unassigns successfully ignoring wrong attempts with sorting and paging" in new BulkAssignmentFixture {
      POST(s"v1/orders/assignees", BulkAssignmentPayload[String](Seq(orderRef1, orderRef2), adminId))

      val unassign = POST(s"v1/orders/assignees/delete?size=1&from=1&sortBy=referenceNumber",
        BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      unassign.status must === (StatusCodes.OK)

      val responseObj = unassign.as[BatchResponse[AllOrders.Root]]
      responseObj.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj.errors mustBe empty
    }

    "warns when order to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignmentPayload[String](Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BatchResponse[AllOrders.Root]]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/orders/watchers" - {
    "adds successfully ignoring duplicates" in new BulkAssignmentFixture {
      val watcherResponse1 = POST(s"v1/orders/watchers", BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      watcherResponse1.status must === (StatusCodes.OK)
      val responseObj1 = watcherResponse1.as[BatchResponse[AllOrders.Root]]
      responseObj1.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj1.errors mustBe empty

      // FIXME
      /*
      val updOrderResponse1 = GET(s"v1/orders/$orderRef1")
      val updOrder1 = updOrderResponse1.withResultTypeOf[FullOrder.Root].result
      updOrder1.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(admin)))
      */

      // Don't complain about duplicates
      val watcherResponse2 = POST(s"v1/orders/watchers", BulkAssignmentPayload[String](Seq(orderRef1, orderRef2), adminId))
      watcherResponse2.status must === (StatusCodes.OK)
      val responseObj2 = watcherResponse2.as[BatchResponse[AllOrders.Root]]
      responseObj2.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj2.errors mustBe empty

      // FIXME
      /*
      val updOrderResponse2 = GET(s"v1/orders/$orderRef1")
      val updOrder2 = updOrderResponse2.withResultTypeOf[FullOrder.Root].result
      updOrder2.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(admin)))

      val updOrderResponse3 = GET(s"v1/orders/$orderRef2")
      val updOrder3 = updOrderResponse3.withResultTypeOf[FullOrder.Root].result
      updOrder3.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(admin)))
      */
    }

    "happens successfully ignoring duplicates with sorting and paging" in new BulkAssignmentFixture {
      val watcherResponse1 = POST(s"v1/orders/watchers?size=1&from=1&sortBy=referenceNumber",
        BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      watcherResponse1.status must === (StatusCodes.OK)
      val responseObj1 = watcherResponse1.as[BatchResponse[AllOrders.Root]]
      responseObj1.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj1.errors mustBe empty
    }

    "warns when order to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/watchers", BulkAssignmentPayload[String](Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BatchResponse[AllOrders.Root]]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/watchers", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/orders/watchers/delete" - {
    "unwatches successfully ignoring wrong attempts" in new BulkAssignmentFixture {
      // Should pass
      val unwatch1 = POST(s"v1/orders/watchers/delete", BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      unwatch1.status must === (StatusCodes.OK)

      POST(s"v1/orders/watchers", BulkAssignmentPayload[String](Seq(orderRef1, orderRef2), adminId))

      val unwatch2 = POST(s"v1/orders/watchers/delete", BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      unwatch2.status must === (StatusCodes.OK)

      // FIXME
      /*
      val updOrder1 = GET(s"v1/orders/$orderRef1")
      updOrder1.status must === (StatusCodes.OK)

      val updOrder1Root = updOrder1.withResultTypeOf[FullOrder.Root].result
      updOrder1Root.assignees mustBe empty

      val updOrder2 = GET(s"v1/orders/$orderRef2")
      updOrder2.status must === (StatusCodes.OK)

      val updOrder2Root = updOrder2.withResultTypeOf[FullOrder.Root].result
      updOrder2Root.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(admin)))
      */
    }

    "unwatches successfully ignoring wrong attempts with sorting and paging" in new BulkAssignmentFixture {
      POST(s"v1/orders/watchers", BulkAssignmentPayload[String](Seq(orderRef1, orderRef2), adminId))

      val unwatch = POST(s"v1/orders/watchers/delete?size=1&from=1&sortBy=referenceNumber",
        BulkAssignmentPayload[String](Seq(orderRef1), adminId))
      unwatch.status must === (StatusCodes.OK)

      val responseObj = unwatch.as[BatchResponse[AllOrders.Root]]
      responseObj.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj.errors mustBe empty
    }

    "warns when order to unwatch not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/watchers/delete", BulkAssignmentPayload[String](Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BatchResponse[AllOrders.Root]]
      responseObj.result must === (getAllOrders)
      responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to unwatch not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/watchers/delete", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  trait StateUpdateFixture {
    (for {
      cust ← * <~ Customers.create(Factories.customer)
      foo  ← * <~ Orders.create(Factories.order.copy(customerId = cust.id, referenceNumber = "foo", state = FraudHold))
      bar  ← * <~ Orders.create(Factories.order.copy(customerId = cust.id, referenceNumber = "bar",
        state = RemorseHold, isLocked = true))
      baz  ← * <~ Orders.create(Factories.order.copy(customerId = cust.id, referenceNumber = "baz", state = ManualHold))
    } yield (cust, foo, bar)).runTxn().futureValue.rightVal
  }

  trait BulkAssignmentFixture {
    val (order1, order2, admin) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      order1   ← * <~ Orders.create(Factories.order.copy(id = 1, referenceNumber = "foo", customerId = customer.id))
      order2   ← * <~ Orders.create(Factories.order.copy(id = 2, referenceNumber = "bar", customerId = customer.id))
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (order1, order2, admin)).runTxn().futureValue.rightVal
    val orderRef1 = order1.referenceNumber
    val orderRef2 = order2.referenceNumber
    val adminId = admin.id
  }
}
