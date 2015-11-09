import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Future

import Extensions._
import models._
import org.json4s.jackson.JsonMethods._
import payloads.RmaAssigneesPayload
import responses.{StoreAdminResponse, ResponseWithFailuresAndMetadata, RmaResponse, RmaLockResponse}
import responses.RmaResponse.FullRmaWithWarnings
import services._
import services.rmas._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.time._
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

class RmaIntegrationTest extends IntegrationTestBase
with HttpSupport
with AutomaticAuth
with SortingAndPaging[RmaResponse.Root] {

  // paging and sorting API
  private var currentCustomer: Customer = _
  private var currentOrder: Order = _

  val uriPrefix = "v1/rmas"

  override def beforeSortingAndPaging() = {
    (for {
      storeAdmin ← StoreAdmins.saveNew(Factories.storeAdmin)
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(
        status = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (customer, order)).run().futureValue match {
      case (cc, co) ⇒
        currentCustomer = cc
        currentOrder = co
    }
  }

  def responseItems = {
    val items = (1 to numOfResults).map { i ⇒
      val future = Rmas.saveNew(Factories.rma.copy(
        referenceNumber = s"RMA-$i",
        orderId = currentOrder.id,
        orderRefNum = currentOrder.referenceNumber,
        customerId = currentCustomer.id)
      ).run()

      future.map(RmaResponse.build(_))
    }

    Future.sequence(items).futureValue
  }

  val sortColumnName = "referenceNumber"

  def responseItemsSort(items: IndexedSeq[RmaResponse.Root]) = items.sortBy(_.referenceNumber)

  def mf = implicitly[scala.reflect.Manifest[RmaResponse.Root]]
  // paging and sorting API end

  "RMAs" - {
    "GET /v1/rmas" - {
      "should return list of RMAs" in new Fixture {
        val response = GET(s"v1/rmas")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[RmaResponse.Root]]]
        root.result.size must === (1)
        root.result.head.referenceNumber must ===(rma.refNum)
      }
    }

    "GET /v1/rmas/customer/:id" - {
      "should return list of RMAs of existing customer" in new Fixture {
        val response = GET(s"v1/rmas/customer/${customer.id}")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[RmaResponse.Root]]]
        root.result.size must === (1)
        root.result.head.referenceNumber must ===(rma.refNum)
      }

      "should return failure for non-existing customer" in new Fixture {
        val response = GET(s"v1/rmas/customer/255")
        response.status must ===(StatusCodes.NotFound)
        response.errors must ===(NotFoundFailure404(Customer, 255).description)
      }
    }

    "GET /v1/rmas/order/:refNum" - {
      "should return list of RMAs of existing order" in new Fixture {
        val response = GET(s"v1/rmas/order/${order.refNum}")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[RmaResponse.Root]]]
        root.result.size must === (1)
        root.result.head.referenceNumber must ===(rma.refNum)
      }

      "should return failure for non-existing order" in new Fixture {
        val response = GET(s"v1/rmas/order/ABC-666")
        response.status must ===(StatusCodes.NotFound)
        response.errors must ===(NotFoundFailure404(Order, "ABC-666").description)
      }
    }

    "GET /v1/rmas/:refNum" - {
      "should return valid RMA by referenceNumber" in new Fixture {
        val response = GET(s"v1/rmas/${rma.refNum}")
        response.status must ===(StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.referenceNumber must ===(rma.refNum)
      }

      "should return 404 if invalid rma is returned" in new Fixture {
        val response = GET(s"v1/rmas/ABC-666")
        response.status must ===(StatusCodes.NotFound)
        response.errors must ===(NotFoundFailure404(Rma, "ABC-666").description)
      }
    }

    "GET /v1/rmas/:refNum/lock" - {
      "returns lock info on locked RMA" in {
        Orders.saveNew(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue
        val rma = Rmas.saveNew(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue
        val admin = StoreAdmins.saveNew(Factories.storeAdmin).run().futureValue

        RmaLockUpdater.lock("ABC-123.1", admin).futureValue

        val response = GET(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaLockResponse.Root]
        root.isLocked must === (true)
        root.lock.head.lockedBy.id must === (admin.id)
      }

      "returns negative lock status on unlocked RMA" in {
        Orders.saveNew(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue
        val rma = Rmas.saveNew(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue

        val response = GET(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaLockResponse.Root]
        root.isLocked must === (false)
        root.lock.isEmpty must === (true)
      }
    }

    "POST /v1/rmas/:refNum/lock" - {
      "successfully locks an RMA" in {
        Orders.saveNew(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue
        val rma = Rmas.saveNew(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue
        StoreAdmins.saveNew(Factories.storeAdmin).run().futureValue

        val response = POST(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val lockedRma = Rmas.findByRefNum(rma.referenceNumber).result.run().futureValue.head
        lockedRma.locked must === (true)

        val locks = RmaLockEvents.findByRma(rma).result.run().futureValue
        locks.length must === (1)
        val lock = locks.head
        lock.lockedBy must === (1)
      }

      "refuses to lock an already locked RMA" in {
        Orders.saveNew(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue
        val rma = Rmas.saveNew(Factories.rma.copy(referenceNumber = "ABC-123.1", locked = true)).run().futureValue

        val response = POST(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.BadRequest)
        response.errors must === (LockedFailure(Rma, rma.referenceNumber).description)
      }

      "avoids race condition" in {
        StoreAdmins.saveNew(Factories.storeAdmin).run().futureValue
        Orders.saveNew(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue
        val rma = Rmas.saveNew(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue

        def request = POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val responses = Seq(0, 1).par.map(_ ⇒ request)
        responses.map(_.status) must contain allOf(StatusCodes.OK, StatusCodes.BadRequest)
        RmaLockEvents.result.run().futureValue.length mustBe 1
      }
    }

    "POST /v1/rmas/:refNum/unlock" - {
      "unlocks an RMA" in {
        StoreAdmins.saveNew(Factories.storeAdmin).run().futureValue
        Orders.saveNew(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue
        val rma = Rmas.saveNew(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue

        POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")
        response.status must ===(StatusCodes.OK)

        val unlockedRma = Rmas.findByRefNum(rma.referenceNumber).result.run().futureValue.head
        unlockedRma.locked must ===(false)
      }

      "refuses to unlock an already unlocked RMA" in {
        Orders.saveNew(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue
        val rma = Rmas.saveNew(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue
        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")

        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(GeneralFailure("Return is not locked").description)
      }
    }

    "GET /v1/rmas/:refNum/expanded" - {
      "should return expanded RMA by referenceNumber" in new Fixture {
        val response = GET(s"v1/rmas/${rma.refNum}/expanded")
        response.status must ===(StatusCodes.OK)

        val root = response.as[RmaResponse.RootExpanded]
        root.referenceNumber must ===(rma.refNum)
        root.order.head.referenceNumber must ===(order.refNum)
      }

      "should return 404 if invalid rma is returned" in new Fixture {
        val response = GET(s"v1/rmas/ABC-666/expanded")
        response.status must ===(StatusCodes.NotFound)
        response.errors must ===(NotFoundFailure404(Rma, "ABC-666").description)

      }
    }

    "POST /v1/rmas/:refNum/assignees" - {
      "can be assigned to RMA" in new Fixture {
        val response = POST(s"v1/rmas/${rma.referenceNumber}/assignees", RmaAssigneesPayload(Seq(storeAdmin.id)))
        response.status must === (StatusCodes.OK)

        val fullRmaWithWarnings = parse(response.bodyText).extract[FullRmaWithWarnings]
        fullRmaWithWarnings.rma.assignees must not be empty
        fullRmaWithWarnings.rma.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
        fullRmaWithWarnings.warnings mustBe empty
      }

      "can be assigned to locked RMA" in new Fixture {
        Rmas.findByRefNum(rma.referenceNumber).map(_.locked).update(true).run().futureValue
        val response = POST(s"v1/rmas/${rma.referenceNumber}/assignees", RmaAssigneesPayload(Seq(storeAdmin.id)))
        response.status must === (StatusCodes.OK)
      }

      "404 if RMA is not found" in new Fixture {
        val response = POST(s"v1/rmas/NOPE/assignees", RmaAssigneesPayload(Seq(storeAdmin.id)))
        response.status must === (StatusCodes.NotFound)
      }

      "warning if assignee is not found" in new Fixture {
        val response = POST(s"v1/rmas/${rma.referenceNumber}/assignees", RmaAssigneesPayload(Seq(1, 999)))
        response.status must === (StatusCodes.OK)

        val fullRmaWithWarnings = parse(response.bodyText).extract[FullRmaWithWarnings]
        fullRmaWithWarnings.rma.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
        fullRmaWithWarnings.warnings mustBe Seq(NotFoundFailure404(StoreAdmin, 999))
      }

      "can be viewed with RMA" in new Fixture {
        val response1 = GET(s"v1/rmas/${rma.referenceNumber}")
        response1.status must === (StatusCodes.OK)
        val responseOrder1 = parse(response1.bodyText).extract[RmaResponse.Root]
        responseOrder1.assignees mustBe empty

        POST(s"v1/rmas/${rma.referenceNumber}/assignees", RmaAssigneesPayload(Seq(storeAdmin.id)))
        val response2 = GET(s"v1/rmas/${rma.referenceNumber}")
        response2.status must === (StatusCodes.OK)

        val responseRma2 = parse(response2.bodyText).extract[RmaResponse.Root]
        responseRma2.assignees must not be empty
        responseRma2.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
      }

      "do not create duplicate records" in new Fixture {
        POST(s"v1/rmas/${rma.referenceNumber}/assignees", RmaAssigneesPayload(Seq(storeAdmin.id)))
        POST(s"v1/rmas/${rma.referenceNumber}/assignees", RmaAssigneesPayload(Seq(storeAdmin.id)))

        RmaAssignments.byRma(rma).result.run().futureValue.size mustBe 1
      }
    }
  }

  trait Fixture {
    val (storeAdmin, customer, order, rma) = (for {
      storeAdmin ← StoreAdmins.saveNew(Factories.storeAdmin)
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(
        status = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
      rma ← Rmas.saveNew(Factories.rma.copy(
        referenceNumber = "ABC-123.1",
        orderId = order.id,
        orderRefNum = order.referenceNumber,
        customerId = customer.id))
    } yield (storeAdmin, customer, order, rma)).run().futureValue
  }
}