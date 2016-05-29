import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.{AlreadyAssignedFailure, AssigneeNotFoundFailure, NotAssignedFailure, NotFoundFailure404}
import models.{Assignment, Assignments, StoreAdmin, StoreAdmins}
import models.customer.Customers
import models.order.{Order, Orders}
import payloads.AssignmentPayloads._
import responses.{AssignmentResponse, BatchMetadata, BatchMetadataSource, BatchResponse, TheResponse}
import util.IntegrationTestBase
import utils.seeds.Seeds.Factories
import scala.concurrent.ExecutionContext.Implicits.global
import utils.db._
import utils.db.DbResultT._
import responses.order.AllOrders

class AssignmentsIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST /v1/orders/:refNum/assignees" - {

    "can be assigned to order" in new Fixture {
      val payload  = AssignmentPayload(assignees = Seq(storeAdmin.id))
      val response = POST(s"v1/orders/${order.refNum}/assignees", payload)
      response.status must ===(StatusCodes.OK)

      val theResponse = response.as[TheResponse[Seq[AssignmentResponse.Root]]]
      theResponse.result.size mustBe 1
      theResponse.result.headOption.value.assignee.id mustBe storeAdmin.id
      theResponse.result.headOption.value.assignmentType mustBe Assignment.Assignee

      theResponse.errors mustBe None
    }

    "extends response with errors if one of store admins is not found" in new Fixture {
      val nonExistentAdminId = 2
      val payload            = AssignmentPayload(assignees = Seq(storeAdmin.id, nonExistentAdminId))
      val response           = POST(s"v1/orders/${order.refNum}/assignees", payload)
      response.status must ===(StatusCodes.OK)

      // TODO - AlreadyAssignedFailure here?
      val theResponse = response.as[TheResponse[Seq[AssignmentResponse.Root]]]
      theResponse.result.size mustBe 1
      theResponse.result.headOption.value.assignee.id mustBe storeAdmin.id
      theResponse.result.headOption.value.assignmentType mustBe Assignment.Assignee

      theResponse.errors.value.size mustBe 1
      theResponse.errors.value.headOption.value mustBe NotFoundFailure404(
          StoreAdmin, nonExistentAdminId).description
    }

    "returns error if order not found" in new Fixture {
      val payload  = AssignmentPayload(assignees = Seq(storeAdmin.id))
      val response = POST(s"v1/orders/NOPE/assignees", payload)
      response.status must ===(StatusCodes.NotFound)
      response.error mustBe NotFoundFailure404(Order, "NOPE").description
    }
  }

  "DELETE /v1/orders/:refNum/assignees" - {

    "can be unassigned from order" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/assignees/${storeAdmin.id}")
      response.status must ===(StatusCodes.OK)

      val theResponse = response.as[Seq[AssignmentResponse.Root]]
      theResponse mustBe 'empty
    }

    "returns error if order not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/NOPE/assignees/${storeAdmin.id}")
      response.status must ===(StatusCodes.NotFound)
      response.error mustBe NotFoundFailure404(Order, "NOPE").description
    }

    "returns error if store admin not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/assignees/666")
      response.status must ===(StatusCodes.NotFound)
      response.error mustBe NotFoundFailure404(StoreAdmin, 666).description
    }

    "returns error if assignment not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/assignees/${secondAdmin.id}")
      response.status must ===(StatusCodes.BadRequest)
      response.error mustBe AssigneeNotFoundFailure(Order,
                                                    order.refNum,
                                                    secondAdmin.id).description
    }
  }

  "POST /v1/orders/assignees" - {

    "can be assigned to multiple orders with graceful error handling" in new BulkAssignmentFixture {
      val payload = BulkAssignmentPayload(entityIds = Seq(order1.refNum, order2.refNum, "NOPE"),
                                          storeAdminId = admin.id)
      val response = POST(s"v1/orders/assignees", payload)
      response.status must ===(StatusCodes.OK)

      val theResponse = response.as[TheResponse[Seq[AllOrders.Root]]]
      theResponse.result.size mustBe 2

      val notFoundFailure = NotFoundFailure404(Order, "NOPE").description
      val alreadyAssignedFailure =
        AlreadyAssignedFailure(Order, order1.refNum, admin.id).description
      val assertFailures =
        Map[String, String]("NOPE" → notFoundFailure, order1.refNum → alreadyAssignedFailure)

      theResponse.errors.value.size mustBe 2
      theResponse.errors.value mustBe assertFailures.values.toList

      val batchAssertion = BatchMetadataSource(Order, Seq(order2.refNum), assertFailures)
      theResponse.batch.value mustBe BatchMetadata(batchAssertion)
    }
  }

  "POST /v1/orders/assignees/delete" - {

    "can be unassigned from multiple orders with graceful error handling" in new BulkAssignmentFixture {
      val payload = BulkAssignmentPayload(entityIds = Seq(order1.refNum, order2.refNum, "NOPE"),
                                          storeAdminId = admin.id)
      val response = POST(s"v1/orders/assignees/delete", payload)
      response.status must ===(StatusCodes.OK)

      val theResponse = response.as[TheResponse[Seq[AllOrders.Root]]]
      theResponse.result.size mustBe 2

      val notFoundFailure    = NotFoundFailure404(Order, "NOPE").description
      val notAssignedFailure = NotAssignedFailure(Order, order2.refNum, admin.id).description
      val assertFailures =
        Map[String, String]("NOPE" → notFoundFailure, order2.refNum → notAssignedFailure)

      theResponse.errors.value.size mustBe 2
      theResponse.errors.value mustBe assertFailures.values.toList

      val batchAssertion = BatchMetadataSource(Order, Seq(order1.refNum), assertFailures)
      theResponse.batch.value mustBe BatchMetadata(batchAssertion)
    }
  }

  trait Fixture {
    val (order, storeAdmin, customer) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      order ← * <~ Orders.create(
                 Factories.order.copy(customerId = customer.id, state = Order.Cart))
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).runTxn().futureValue.rightVal
  }

  trait AssignmentFixture extends Fixture {
    val (assignee, secondAdmin) = (for {
      assignee ← * <~ Assignments.create(Assignment(referenceType = Assignment.Order,
                                                    referenceId = order.id,
                                                    storeAdminId = storeAdmin.id,
                                                    assignmentType = Assignment.Assignee))
      secondAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (assignee, secondAdmin)).runTxn().futureValue.rightVal
  }

  trait BulkAssignmentFixture {
    val (order1, order2, admin) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      order1 ← * <~ Orders.create(Factories.order.copy(id = 1,
                                                       referenceNumber = "foo",
                                                       customerId = customer.id))
      order2 ← * <~ Orders.create(Factories.order.copy(id = 2,
                                                       referenceNumber = "bar",
                                                       customerId = customer.id))
      admin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      assignee ← * <~ Assignments.create(Assignment(referenceType = Assignment.Order,
                                                    referenceId = order1.id,
                                                    storeAdminId = admin.id,
                                                    assignmentType = Assignment.Assignee))
    } yield (order1, order2, admin)).runTxn().futureValue.rightVal

    val orderRef1 = order1.referenceNumber
    val orderRef2 = order2.referenceNumber
    val adminId   = admin.id
  }
}
