import java.time.Instant
import akka.http.scaladsl.model.StatusCodes

import cats.data.Xor
import cats.implicits._
import models.Rma._
import models._
import payloads._
import responses.ResponseWithFailuresAndMetadata.BulkRmaUpdateResponse
import responses.{StoreAdminResponse, RmaResponse, AllRmas}
import services.rmas.RmaQueries
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import utils.time._

class AllRmasIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[AllRmas.Root]
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import api._

  // paging and sorting API
  def uriPrefix = "v1/rmas"

  def responseItems = {
    val items = (1 to numOfResults).map { i ⇒
      val orderRefNum = Factories.randomString(10)

      val dbio = for {
        customer ← Customers.saveNew(Factories.generateCustomer)
        order ← Orders.saveNew(Factories.order.copy(
          customerId = customer.id,
          referenceNumber = orderRefNum,
          status = Order.RemorseHold,
          remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
        rma ← Rmas.saveNew(Factories.rma.copy(
          customerId = customer.id,
          orderId = order.id,
          orderRefNum = orderRefNum,
          referenceNumber = s"RMA-$i"
        ))
      } yield (customer, rma)

      dbio.flatMap { case (customer, rma) ⇒
        AllRmas.build(rma, customer, None)
      }
    }

    DBIO.sequence(items).transactionally.run().futureValue
  }
  val sortColumnName = "referenceNumber"

  def responseItemsSort(items: IndexedSeq[AllRmas.Root]) = items.sortBy(_.referenceNumber)

  def mf = implicitly[scala.reflect.Manifest[AllRmas.Root]]
  // paging and sorting API end

  def getAllRmas: Seq[AllRmas.Root] = {
    RmaQueries.findAll(Rmas).result.run().futureValue match {
      case Xor.Left(s)    ⇒ fail(s.toList.mkString(";"))
      case Xor.Right(seq) ⇒ seq
    }
  }

  "POST /v1/rmas/assignees" - {
    "assigns successfully ignoring duplicates" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/rmas/assignees", RmaBulkAssigneesPayload(Seq(rmaRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BulkRmaUpdateResponse]
      responseObj1.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj1.errors mustBe empty

      val updOrderResponse1 = GET(s"v1/rmas/$rmaRef1")
      val updRma1 = updOrderResponse1.as[RmaResponse.Root]
      updRma1.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))

      // Don't complain about duplicates
      val assignResponse2 = POST(s"v1/rmas/assignees", RmaBulkAssigneesPayload(Seq(rmaRef1, rmaRef2), adminId))
      assignResponse2.status must === (StatusCodes.OK)
      val responseObj2 = assignResponse2.as[BulkRmaUpdateResponse]
      responseObj2.result.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj2.errors mustBe empty

      val updOrderResponse2 = GET(s"v1/rmas/$rmaRef1")
      val updRma2 = updOrderResponse2.as[RmaResponse.Root]
      updRma2.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))

      val updOrderResponse3 = GET(s"v1/rmas/$rmaRef2")
      val updOrder3 = updOrderResponse3.as[RmaResponse.Root]
      updOrder3.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
    }

    "happens successfully ignoring duplicates with sorting and paging" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/rmas/assignees?size=1&from=1&sortBy=referenceNumber",
        RmaBulkAssigneesPayload(Seq(rmaRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BulkRmaUpdateResponse]
      responseObj1.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj1.errors mustBe empty
    }

    "errors when RMA to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/rmas/assignees", RmaBulkAssigneesPayload(Seq(rmaRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkRmaUpdateResponse]
      responseObj.result must === (getAllRmas)
      responseObj.errors.value must === (NotFoundFailure404(Rma, "NOPE").description)
    }

    "errors when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/rmas/assignees", RmaBulkAssigneesPayload(Seq(rmaRef1), 777))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkRmaUpdateResponse]
      responseObj.result must === (getAllRmas)
      responseObj.errors.value must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  "POST /v1/rmas/assignees/delete" - {
    "unassigns successfully ignoring wrong attempts" in new BulkAssignmentFixture {
      // Should pass
      val unassign1 = POST(s"v1/rmas/assignees/delete", RmaBulkAssigneesPayload(Seq(rmaRef1), adminId))
      unassign1.status must === (StatusCodes.OK)

      POST(s"v1/rmas/assignees", RmaBulkAssigneesPayload(Seq(rmaRef1, rmaRef2), adminId))

      val unassign2 = POST(s"v1/rmas/assignees/delete", RmaBulkAssigneesPayload(Seq(rmaRef1), adminId))
      unassign2.status must === (StatusCodes.OK)

      val updRma1 = GET(s"v1/rmas/$rmaRef1")
      updRma1.status must === (StatusCodes.OK)

      val updRma1Root = updRma1.as[RmaResponse.Root]
      updRma1Root.assignees mustBe empty

      val updRma2 = GET(s"v1/rmas/$rmaRef2")
      updRma2.status must === (StatusCodes.OK)

      val updRma2Root = updRma2.as[RmaResponse.Root]
      updRma2Root.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
    }

    "unassigns successfully ignoring wrong attempts with sorting and paging" in new BulkAssignmentFixture {
      POST(s"v1/rmas/assignees", RmaBulkAssigneesPayload(Seq(rmaRef1, rmaRef2), adminId))

      val unassign = POST(s"v1/rmas/assignees/delete?size=1&from=1&sortBy=referenceNumber",
        RmaBulkAssigneesPayload(Seq(rmaRef1), adminId))
      unassign.status must === (StatusCodes.OK)

      val responseObj = unassign.as[BulkRmaUpdateResponse]
      responseObj.result.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj.errors mustBe empty
    }

    "errors when RMA to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/rmas/assignees/delete", RmaBulkAssigneesPayload(Seq(rmaRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkRmaUpdateResponse]
      responseObj.result must === (getAllRmas)
      responseObj.errors.value must === (NotFoundFailure404(Rma, "NOPE").description)
    }

    "errors when admin to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/rmas/assignees/delete", RmaBulkAssigneesPayload(Seq(rmaRef1), 777))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkRmaUpdateResponse]
      responseObj.result must === (getAllRmas)
      responseObj.errors.value must === (NotFoundFailure404(StoreAdmin, 777).description)
    }
  }

  trait BulkAssignmentFixture {
    val (rma1, rma2, admin) = (for {
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(id = 1, referenceNumber = "foo", customerId = customer.id))
      rma1 ← Rmas.saveNew(Factories.rma.copy(id = 1, referenceNumber = "foo", customerId = customer.id))
      rma2 ← Rmas.saveNew(Factories.rma.copy(id = 2, referenceNumber = "bar", customerId = customer.id))
      admin ← StoreAdmins.saveNew(Factories.storeAdmin)
    } yield (rma1, rma2, admin)).run().futureValue

    val rmaRef1 = rma1.referenceNumber
    val rmaRef2 = rma2.referenceNumber
    val adminId = admin.id
  }
}