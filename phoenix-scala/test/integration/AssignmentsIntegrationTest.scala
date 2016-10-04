import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.AssignmentFailures._
import failures.NotFoundFailure404
import models._
import models.account._
import models.admin._
import models.cord._
import payloads.AssignmentPayloads._
import responses._
import responses.cord.AllOrders
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class AssignmentsIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "POST /v1/orders/:refNum/assignees" - {

    "can be assigned to order" in new Order_Baked {
      val payload  = AssignmentPayload(assignees = Seq(storeAdmin.accountId))
      val response = POST(s"v1/orders/${order.refNum}/assignees", payload)
      response.status must === (StatusCodes.OK)

      val theResponse = response.as[TheResponse[Seq[AssignmentResponse.Root]]]
      theResponse.result.size mustBe 1
      theResponse.result.headOption.value.assignee.id mustBe storeAdmin.accountId
      theResponse.result.headOption.value.assignmentType mustBe Assignment.Assignee

      theResponse.errors mustBe None
    }

    "extends response with errors if one of store admins is not found" in new Order_Baked {
      val nonExistentAdminId = 100
      val payload            = AssignmentPayload(assignees = Seq(storeAdmin.accountId, nonExistentAdminId))
      val response           = POST(s"v1/orders/${order.refNum}/assignees", payload)
      response.status must === (StatusCodes.OK)

      val theResponse = response.as[TheResponse[Seq[AssignmentResponse.Root]]]
      theResponse.result.size mustBe 1
      theResponse.result.headOption.value.assignee.id mustBe storeAdmin.accountId
      theResponse.result.headOption.value.assignmentType mustBe Assignment.Assignee

      theResponse.errors.value.size mustBe 1
      theResponse.errors.value.headOption.value mustBe NotFoundFailure404(User, nonExistentAdminId).description
    }

    "returns error if order not found" in new Order_Baked {
      val payload  = AssignmentPayload(assignees = Seq(storeAdmin.accountId))
      val response = POST(s"v1/orders/NOPE/assignees", payload)
      response.status must === (StatusCodes.NotFound)
      response.error mustBe NotFoundFailure404(Order, "NOPE").description
    }
  }

  "DELETE /v1/orders/:refNum/assignees" - {

    "can be unassigned from order" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/assignees/${storeAdmin.accountId}")
      response.status must === (StatusCodes.OK)

      val theResponse = response.as[Seq[AssignmentResponse.Root]]
      theResponse mustBe 'empty
    }

    "returns error if order not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/NOPE/assignees/${storeAdmin.accountId}")
      response.status must === (StatusCodes.NotFound)
      response.error mustBe NotFoundFailure404(Order, "NOPE").description
    }

    "returns error if store admin not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/assignees/666")
      response.status must === (StatusCodes.NotFound)
      response.error mustBe NotFoundFailure404(User, 666).description
    }

    "returns error if assignment not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/assignees/${secondAdmin.id}")
      response.status must === (StatusCodes.BadRequest)
      response.error mustBe AssigneeNotFoundFailure(Order, order.refNum, secondAdmin.id).description
    }
  }

  "POST /v1/orders/assignees" - {

    "can be assigned to multiple orders with graceful error handling" in new BulkAssignmentFixture {
      val payload = BulkAssignmentPayload(entityIds = Seq(order1.refNum, order2.refNum, "NOPE"),
                                          storeAdminId = storeAdmin.accountId)
      val response = POST(s"v1/orders/assignees", payload)
      response.status must === (StatusCodes.OK)

      val theResponse = response.as[TheResponse[Seq[AllOrders.Root]]]
      theResponse.result.size mustBe 2

      val notFoundFailure = NotFoundFailure404(Order, "NOPE").description
      val alreadyAssignedFailure =
        AlreadyAssignedFailure(Order, order1.refNum, storeAdmin.accountId).description
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
                                          storeAdminId = storeAdmin.accountId)
      val response = POST(s"v1/orders/assignees/delete", payload)
      response.status must === (StatusCodes.OK)

      val theResponse = response.as[TheResponse[Seq[AllOrders.Root]]]
      theResponse.result.size mustBe 2

      val notFoundFailure = NotFoundFailure404(Order, "NOPE").description
      val notAssignedFailure =
        NotAssignedFailure(Order, order2.refNum, storeAdmin.accountId).description
      val assertFailures =
        Map[String, String]("NOPE" → notFoundFailure, order2.refNum → notAssignedFailure)

      theResponse.errors.value.size mustBe 2
      theResponse.errors.value mustBe assertFailures.values.toList

      val batchAssertion = BatchMetadataSource(Order, Seq(order1.refNum), assertFailures)
      theResponse.batch.value mustBe BatchMetadata(batchAssertion)
    }
  }

  trait AssignmentFixture extends Order_Baked {
    val (assignee, secondAdmin) = (for {
      assignee ← * <~ Assignments.create(
                    Assignment(referenceType = Assignment.Order,
                               referenceId = order.id,
                               storeAdminId = storeAdmin.accountId,
                               assignmentType = Assignment.Assignee))
      account ← * <~ Accounts.create(Account())
      secondAdmin ← * <~ Users.create(
                       Factories.storeAdmin
                         .copy(accountId = account.id, email = "a@b.c".some, name = "Admin2".some))
      custData ← * <~ AdminsData.create(AdminData(userId = secondAdmin.id, accountId = account.id))
    } yield (assignee, secondAdmin)).gimme
  }

  trait BulkAssignmentFixture extends Customer_Seed with StoreAdmin_Seed {
    val (order1, order2) = (for {
      cart ← * <~ Carts.create(
                Factories.cart.copy(accountId = customer.accountId, referenceNumber = "foo"))
      order1 ← * <~ Orders.createFromCart(cart)
      cart ← * <~ Carts.create(
                Factories.cart.copy(accountId = customer.accountId, referenceNumber = "bar"))
      order2 ← * <~ Orders.createFromCart(cart)
      assignee ← * <~ Assignments.create(
                    Assignment(referenceType = Assignment.Order,
                               referenceId = order1.id,
                               storeAdminId = storeAdmin.accountId,
                               assignmentType = Assignment.Assignee))
    } yield (order1, order2)).gimme

    val orderRef1 = order1.referenceNumber
    val orderRef2 = order2.referenceNumber
    val adminId   = storeAdmin.accountId
  }
}
