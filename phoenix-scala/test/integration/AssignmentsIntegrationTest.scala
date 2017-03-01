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
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Factories

class AssignmentsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "POST /v1/orders/:refNum/assignees" - {

    "can be assigned to order" in new Order_Baked {
      val response = ordersApi(order.refNum)
        .assign(AssignmentPayload(assignees = Seq(storeAdmin.accountId)))
        .as[TheResponse[Seq[AssignmentResponse.Root]]]

      response.result must have size 1

      private val assignment = response.result.headOption.value
      assignment.assignee.id mustBe storeAdmin.accountId
      assignment.assignmentType mustBe Assignment.Assignee

      response.errors mustBe None
    }

    "extends response with errors if one of store admins is not found" in new Order_Baked {
      // TODO - AlreadyAssignedFailure here?
      val response = ordersApi(order.refNum)
        .assign(AssignmentPayload(assignees = Seq(storeAdmin.accountId, 666)))
        .asThe[Seq[AssignmentResponse.Root]]

      response.result must have size 1

      private val assignment = response.result.headOption.value
      assignment.assignee.id must === (storeAdmin.id)
      assignment.assignmentType must === (Assignment.Assignee)

      private val errors = response.errors.value
      errors must have size 1
      errors.headOption.value must === (NotFoundFailure404(User, 666).description)
    }

    "returns error if order not found" in new Order_Baked {
      ordersApi("NOPE")
        .assign(AssignmentPayload(assignees = Seq(storeAdmin.accountId)))
        .mustFailWith404(NotFoundFailure404(Order, "NOPE"))
    }
  }

  "DELETE /v1/orders/:refNum/assignees" - {

    "can be unassigned from order" in new AssignmentFixture {
      val response = ordersApi(order.refNum).unassign(storeAdmin.accountId)

      val theResponse = response.as[Seq[AssignmentResponse.Root]]
      theResponse mustBe 'empty
    }

    "returns error if order not found" in new AssignmentFixture {
      ordersApi("NOPE")
        .unassign(storeAdmin.accountId)
        .mustFailWith404(NotFoundFailure404(Order, "NOPE"))
    }

    "returns error if store admin not found" in new AssignmentFixture {
      ordersApi(order.refNum).unassign(666).mustFailWith404(NotFoundFailure404(User, 666))
    }

    "returns error if assignment not found" in new AssignmentFixture {
      ordersApi(order.refNum)
        .unassign(secondAdmin.id)
        .mustFailWith400(AssigneeNotFoundFailure(Order, order.refNum, secondAdmin.id))
    }
  }

  "POST /v1/orders/assignees" - {

    "can be assigned to multiple orders with graceful error handling" in new BulkAssignmentFixture {
      private val response = ordersApi
        .assign(BulkAssignmentPayload(entityIds = Seq(order1.refNum, order2.refNum, "NOPE"),
                                      storeAdminId = storeAdmin.accountId))
        .as[TheResponse[Seq[AllOrders.Root]]]

      response.result.size mustBe 2

      private val assertFailures =
        Map[String, String]("NOPE" → NotFoundFailure404(Order, "NOPE").description,
                            order1.refNum → AlreadyAssignedFailure(
                                Order,
                                order1.refNum,
                                storeAdmin.accountId).description)

      response.errors.value must contain theSameElementsAs assertFailures.values

      val batchAssertion = BatchMetadataSource(Order, Seq(order2.refNum), assertFailures)
      response.batch.value mustBe BatchMetadata(batchAssertion)
    }
  }

  "POST /v1/orders/assignees/delete" - {

    "can be unassigned from multiple orders with graceful error handling" in new BulkAssignmentFixture {
      private val response = ordersApi
        .unassign(BulkAssignmentPayload(entityIds = Seq(order1.refNum, order2.refNum, "NOPE"),
                                        storeAdminId = storeAdmin.accountId))
        .as[TheResponse[Seq[AllOrders.Root]]]

      response.result must have size 2

      private val assertFailures =
        Map[String, String]("NOPE" → NotFoundFailure404(Order, "NOPE").description,
                            order2.refNum → NotAssignedFailure(Order,
                                                               order2.refNum,
                                                               storeAdmin.accountId).description)

      response.errors.value must contain theSameElementsAs assertFailures.values

      val batchAssertion = BatchMetadataSource(Order, Seq(order1.refNum), assertFailures)
      response.batch.value must === (BatchMetadata(batchAssertion))
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
      custData ← * <~ AdminsData.create(
                    AdminData(userId = secondAdmin.id,
                              accountId = account.id,
                              scope = Scope.current))
    } yield (assignee, secondAdmin)).gimme
  }

  trait BulkAssignmentFixture extends Customer_Seed with StoreAdmin_Seed {
    val (order1, order2) = (for {
      cart ← * <~ Carts.create(
                Factories
                  .cart(Scope.current)
                  .copy(accountId = customer.accountId, referenceNumber = "foo"))
      order1 ← * <~ Orders.createFromCart(cart, None)
      cart ← * <~ Carts.create(
                Factories
                  .cart(Scope.current)
                  .copy(accountId = customer.accountId, referenceNumber = "bar"))
      order2 ← * <~ Orders.createFromCart(cart, None)
      _ ← * <~ Assignments.create(
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
