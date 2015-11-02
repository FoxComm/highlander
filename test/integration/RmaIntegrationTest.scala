import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Future

import Extensions._
import models._
import org.json4s.jackson.JsonMethods._
import payloads.{RmaCreatePayload, RmaAssigneesPayload}
import responses.{AllRmas, StoreAdminResponse, ResponseWithFailuresAndMetadata, RmaResponse, RmaLockResponse}
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
  with AutomaticAuth {

  "RMAs" - {
    "GET /v1/rmas" - {
      "should return list of RMAs" in new Fixture {
        val response = GET(s"v1/rmas")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[AllRmas.Root]]]
        root.result.size must === (1)
        root.result.head.referenceNumber must ===(rma.refNum)
      }
    }

    "GET /v1/rmas/customer/:id" - {
      "should return list of RMAs of existing customer" in new Fixture {
        val response = GET(s"v1/rmas/customer/${customer.id}")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[AllRmas.Root]]]
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

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[AllRmas.Root]]]
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

    "POST /v1/rmas" - {
      "successfully creates new RMA" in new Fixture {
        val response = POST(s"v1/rmas", RmaCreatePayload(orderRefNum = order.refNum, rmaType = Rma.Standard))
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.referenceNumber must === (s"${order.refNum}.2")
        root.customer.head.id must === (order.customerId)
        root.storeAdmin.head.id must === (storeAdmin.id)
      }

      "fails to create RMA with invalid order refNum provided" in new Fixture {
        val response = POST(s"v1/rmas", RmaCreatePayload(orderRefNum = "ABC-666", rmaType = Rma.Standard))
        response.status must === (StatusCodes.NotFound)
        response.errors must === (NotFoundFailure404(Order, "ABC-666").description)
      }
    }

    "GET /v1/rmas/:refNum/lock" - {
      "returns lock info on locked RMA" in {
        Customers.create(Factories.customer).run().futureValue.rightVal
        Orders.create(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue.rightVal
        val rma = Rmas.create(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue.rightVal
        val admin = StoreAdmins.create(Factories.storeAdmin).run().futureValue.rightVal

        RmaLockUpdater.lock("ABC-123.1", admin).futureValue

        val response = GET(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaLockResponse.Root]
        root.isLocked must === (true)
        root.lock.head.lockedBy.id must === (admin.id)
      }

      "returns negative lock status on unlocked RMA" in {
        Customers.create(Factories.customer).run().futureValue.rightVal
        Orders.create(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue.rightVal
        val rma = Rmas.create(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue.rightVal

        val response = GET(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaLockResponse.Root]
        root.isLocked must === (false)
        root.lock.isEmpty must === (true)
      }
    }

    "POST /v1/rmas/:refNum/lock" - {
      "successfully locks an RMA" in new Fixture {
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
        Customers.create(Factories.customer).run().futureValue.rightVal
        Orders.create(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue.rightVal
        val rma = Rmas.create(Factories.rma.copy(referenceNumber = "ABC-123.1", locked = true)).run().futureValue.rightVal

        val response = POST(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.BadRequest)
        response.errors must === (LockedFailure(Rma, rma.referenceNumber).description)
      }

      "avoids race condition" in new Fixture {
        def request = POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val responses = Seq(0, 1).par.map(_ ⇒ request)
        responses.map(_.status) must contain allOf(StatusCodes.OK, StatusCodes.BadRequest)
        RmaLockEvents.result.run().futureValue.length mustBe 1
      }
    }

    "POST /v1/rmas/:refNum/unlock" - {
      "unlocks an RMA" in new Fixture {
        POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")
        response.status must ===(StatusCodes.OK)

        val unlockedRma = Rmas.findByRefNum(rma.referenceNumber).result.run().futureValue.head
        unlockedRma.locked must ===(false)
      }

      "refuses to unlock an already unlocked RMA" in new Fixture {
        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")

        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(NotLockedFailure(Rma, rma.refNum).description)
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
      storeAdmin ← StoreAdmins.create(Factories.storeAdmin).map(rightValue)
      customer ← Customers.create(Factories.customer).map(rightValue)
      order ← Orders.create(Factories.order.copy(
        status = Order.RemorseHold,
        customerId = customer.id,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30)))).map(rightValue)
      rma ← Rmas.create(Factories.rma.copy(
        orderId = order.id,
        orderRefNum = order.referenceNumber,
        customerId = customer.id)).map(rightValue)
    } yield (storeAdmin, customer, order, rma)).run().futureValue
  }
}
