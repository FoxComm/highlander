import failures.AssignmentFailures._
import failures.NotFoundFailure404
import models._
import models.cord._
import payloads.AssignmentPayloads._
import responses._
import responses.cord.AllOrders
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class AssignmentsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "POST /v1/orders/:refNum/assignees" - {

    "can be assigned to order" in new Order_Baked {
      val payload  = AssignmentPayload(assignees = Seq(storeAdmin.id))
      val response = ordersApi(order.refNum).assign(payload)

      val theResponse = response.as[TheResponse[Seq[AssignmentResponse.Root]]]
      theResponse.result.size mustBe 1
      theResponse.result.headOption.value.assignee.id mustBe storeAdmin.id
      theResponse.result.headOption.value.assignmentType mustBe Assignment.Assignee

      theResponse.errors mustBe None
    }

    "extends response with errors if one of store admins is not found" in new Order_Baked {
      // TODO - AlreadyAssignedFailure here?
      val theResponse = ordersApi(order.refNum)
        .assign(AssignmentPayload(assignees = Seq(storeAdmin.id, 666)))
        .asThe[Seq[AssignmentResponse.Root]]
      theResponse.result.size mustBe 1
      theResponse.result.headOption.value.assignee.id mustBe storeAdmin.id
      theResponse.result.headOption.value.assignmentType mustBe Assignment.Assignee

      theResponse.errors.value.size mustBe 1
      theResponse.errors.value.headOption.value must === (
          NotFoundFailure404(StoreAdmin, 666).description)
    }

    "returns error if order not found" in new Order_Baked {
      ordersApi("NOPE")
        .assign(AssignmentPayload(assignees = Seq(storeAdmin.id)))
        .mustFailWith404(NotFoundFailure404(Order, "NOPE"))
    }
  }

  "DELETE /v1/orders/:refNum/assignees" - {

    "can be unassigned from order" in new AssignmentFixture {
      val response = ordersApi(order.refNum).unassign(storeAdmin.id)

      val theResponse = response.as[Seq[AssignmentResponse.Root]]
      theResponse mustBe 'empty
    }

    "returns error if order not found" in new AssignmentFixture {
      ordersApi("NOPE").unassign(storeAdmin.id).mustFailWith404(NotFoundFailure404(Order, "NOPE"))
    }

    "returns error if store admin not found" in new AssignmentFixture {
      ordersApi(order.refNum).unassign(666).mustFailWith404(NotFoundFailure404(StoreAdmin, 666))
    }

    "returns error if assignment not found" in new AssignmentFixture {
      ordersApi(order.refNum)
        .unassign(secondAdmin.id)
        .mustFailWith400(AssigneeNotFoundFailure(Order, order.refNum, secondAdmin.id))
    }
  }

  "POST /v1/orders/assignees" - {

    "can be assigned to multiple orders with graceful error handling" in new BulkAssignmentFixture {
      val payload = BulkAssignmentPayload(entityIds = Seq(order1.refNum, order2.refNum, "NOPE"),
                                          storeAdminId = storeAdmin.id)

      val theResponse = ordersApi.assign(payload).as[TheResponse[Seq[AllOrders.Root]]]
      theResponse.result.size mustBe 2

      val notFoundFailure = NotFoundFailure404(Order, "NOPE").description
      val alreadyAssignedFailure =
        AlreadyAssignedFailure(Order, order1.refNum, storeAdmin.id).description
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
                                          storeAdminId = storeAdmin.id)
      val theResponse = ordersApi.unassign(payload).as[TheResponse[Seq[AllOrders.Root]]]
      theResponse.result.size mustBe 2

      val notFoundFailure    = NotFoundFailure404(Order, "NOPE").description
      val notAssignedFailure = NotAssignedFailure(Order, order2.refNum, storeAdmin.id).description
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
                               storeAdminId = storeAdmin.id,
                               assignmentType = Assignment.Assignee))
      secondAdmin ← * <~ StoreAdmins.create(
                       Factories.storeAdmin.copy(email = "a@b.c", name = "Admin2"))
    } yield (assignee, secondAdmin)).gimme
  }

  trait BulkAssignmentFixture extends Customer_Seed with StoreAdmin_Seed {
    val (order1, order2) = (for {
      cart ← * <~ Carts.create(
                Factories.cart.copy(customerId = customer.id, referenceNumber = "foo"))
      order1 ← * <~ Orders.createFromCart(cart)
      cart ← * <~ Carts.create(
                Factories.cart.copy(customerId = customer.id, referenceNumber = "bar"))
      order2 ← * <~ Orders.createFromCart(cart)
      assignee ← * <~ Assignments.create(
                    Assignment(referenceType = Assignment.Order,
                               referenceId = order1.id,
                               storeAdminId = storeAdmin.id,
                               assignmentType = Assignment.Assignee))
    } yield (order1, order2)).gimme

    val orderRef1 = order1.referenceNumber
    val orderRef2 = order2.referenceNumber
    val adminId   = storeAdmin.id
  }
}
