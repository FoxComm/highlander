import akka.http.scaladsl.model.StatusCodes
import Extensions._
import models.{Assignment, Assignments, StoreAdmins, StoreAdmin}
import models.customer.Customers
import models.order.{Orders, Order}
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import responses.BatchResponse
import responses.order.AllOrders
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories

import scala.concurrent.ExecutionContext.Implicits.global

class AssignmentsIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  "POST /v1/orders/:refNum/assignees" - {
    pending

    "can be assigned to order" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", AssignmentPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      // FIXME
      /*
      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.warnings mustBe empty

      OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
      */
    }

    "can be assigned to locked order" in new LockedOrderFixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", AssignmentPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      // FIXME
      //OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "404 if order is not found" in new Fixture {
      val response = POST(s"v1/orders/NOPE/assignees", AssignmentPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "warning if assignee is not found" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", AssignmentPayload(Seq(1, 999)))
      response.status must === (StatusCodes.OK)

      // FIXME
      /*
      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.errors.value must === (List(NotFoundFailure404(StoreAdmin, 999).description))
      */
    }

    "can be viewed with order" in new Fixture {
      // FIXME
      /*
      val response1 = GET(s"v1/orders/${order.referenceNumber}")
      response1.status must === (StatusCodes.OK)
      val responseOrder1 = response1.withResultTypeOf[FullOrder.Root].result
      responseOrder1.assignees mustBe empty

      POST(s"v1/orders/${order.referenceNumber}/assignees", AssignmentPayload(Seq(storeAdmin.id)))
      val response2 = GET(s"v1/orders/${order.referenceNumber}")
      response2.status must === (StatusCodes.OK)
      val responseOrder2 = response2.withResultTypeOf[FullOrder.Root].result
      responseOrder2.assignees must not be empty
      responseOrder2.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))

      OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
      */
    }

    "do not create duplicate records" in new Fixture {
      POST(s"v1/orders/${order.referenceNumber}/assignees", AssignmentPayload(Seq(storeAdmin.id)))
      POST(s"v1/orders/${order.referenceNumber}/assignees", AssignmentPayload(Seq(storeAdmin.id)))

      // FIXME
      //OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
    }
  }

  "DELETE /v1/orders/:refNum/assignees/:assigneeId/delete" - {
    pending

    "can be unassigned from order" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/assignees/${storeAdmin.id}")
      response.status must === (StatusCodes.OK)

      // FIXME
      /*
      val root = response.as[FullOrder.Root]
      root.assignees.filter(_.assignee.id == storeAdmin.id) mustBe empty

      OrderAssignments.byOrder(order).result.run().futureValue mustBe empty
      */
    }

    "400 if assignee is not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/assignees/${secondAdmin.id}")
      response.status must === (StatusCodes.BadRequest)
      //response.error must === (OrderAssigneeNotFound(order.referenceNumber, secondAdmin.id).description)
    }

    "404 if order is not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/NOPE/assignees/${storeAdmin.id}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "404 if storeAdmin is not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/assignees/555")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 555).description)
    }
  }

  "POST /v1/orders/:refNum/watchers" - {
    pending

    "can be added to order" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/watchers", AssignmentPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      // FIXME
      /*
      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.warnings mustBe empty

      OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
      */
    }

    "can be added to locked order" in new LockedOrderFixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/watchers", AssignmentPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      // FIXME
      //OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "404 if order is not found" in new Fixture {
      val response = POST(s"v1/orders/NOPE/watchers", AssignmentPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "warning if watcher is not found" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/watchers", AssignmentPayload(Seq(1, 999)))
      response.status must === (StatusCodes.OK)

      // FIXME
      /*
      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.errors.value must === (List(NotFoundFailure404(StoreAdmin, 999).description))
      */
    }

    "can be viewed with order" in new Fixture {
      // FIXME
      /*
      val response1 = GET(s"v1/orders/${order.referenceNumber}")
      response1.status must === (StatusCodes.OK)
      val responseOrder1 = response1.withResultTypeOf[FullOrder.Root].result
      responseOrder1.watchers mustBe empty

      POST(s"v1/orders/${order.referenceNumber}/watchers", AssignmentPayload(Seq(storeAdmin.id)))
      val response2 = GET(s"v1/orders/${order.referenceNumber}")
      response2.status must === (StatusCodes.OK)
      val responseOrder2 = response2.withResultTypeOf[FullOrder.Root].result
      responseOrder2.watchers must not be empty
      responseOrder2.watchers.map(_.watcher) mustBe Seq(StoreAdminResponse.build(storeAdmin))

      OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
      */
    }

    "do not create duplicate records" in new Fixture {
      POST(s"v1/orders/${order.referenceNumber}/watchers", AssignmentPayload(Seq(storeAdmin.id)))
      POST(s"v1/orders/${order.referenceNumber}/watchers", AssignmentPayload(Seq(storeAdmin.id)))

      // FIXME
      //OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
    }
  }

  "DELETE /v1/orders/:refNum/watchers/:watcherId/delete" - {
    pending

    "can be removed from order watchers" in new WatcherFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/watchers/${storeAdmin.id}")
      response.status must === (StatusCodes.OK)

      // FIXME
      /*
      val root = response.ignoreFailuresAndGiveMe[FullOrder.Root]
      root.assignees.filter(_.assignee.id == storeAdmin.id) mustBe empty

      OrderWatchers.byOrder(order).result.run().futureValue mustBe empty
      */
    }

    "400 if watcher is not found" in new WatcherFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/watchers/${secondAdmin.id}")
      response.status must === (StatusCodes.BadRequest)
      //response.error must === (OrderWatcherNotFound(order.referenceNumber, secondAdmin.id).description)
    }

    "404 if order is not found" in new WatcherFixture {
      val response = DELETE(s"v1/orders/NOPE/watchers/${storeAdmin.id}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "404 if storeAdmin is not found" in new WatcherFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/watchers/555")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 555).description)
    }
  }

  "POST /v1/orders/assignees" - {
    pending

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
      //responseObj.result must === (getAllOrders)
      //responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/orders/assignees/delete" - {
    pending

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
      //val responseObj = response.as[BatchResponse[AllOrders.Root]]
      //responseObj.result must === (getAllOrders)
      //responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/orders/watchers" - {
    pending

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
      //responseObj.result must === (getAllOrders)
      //responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/watchers", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/orders/watchers/delete" - {
    pending

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
      //responseObj.result must === (getAllOrders)
      //responseObj.errors.value.head must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "errors when admin to unwatch not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/watchers/delete", BulkAssignmentPayload[String](Seq(orderRef1), 777))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  trait Fixture {
    val (order, storeAdmin, customer) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      order      ← * <~ Orders.create(Factories.order.copy(customerId = customer.id, state = Order.Cart))
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).runTxn().futureValue.rightVal
  }

  trait LockedOrderFixture {
    val (order, storeAdmin) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      order      ← * <~ Orders.create(Factories.order.copy(isLocked = true, customerId = customer.id))
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, storeAdmin)).runTxn().futureValue.rightVal
  }

  trait AssignmentFixture extends Fixture {
    val (assignee, secondAdmin) = (for {
      assignee    ← * <~ Assignments.create(Assignment(referenceType = Assignment.Order, referenceId = order.id,
        storeAdminId = storeAdmin.id, assignmentType = Assignment.Assignee))
      secondAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (assignee, secondAdmin)).runTxn().futureValue.rightVal
  }

  trait WatcherFixture extends Fixture {
    val (watcher, secondAdmin) = (for {
      watcher     ← * <~ Assignments.create(Assignment(referenceType = Assignment.Order, referenceId = order.id,
        storeAdminId = storeAdmin.id, assignmentType = Assignment.Watcher))
      secondAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (watcher, secondAdmin)).runTxn().futureValue.rightVal
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