import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Future

import Extensions._
import models._
import responses.{ResponseWithFailuresAndMetadata, RmaResponse}
import services.{GeneralFailure, LockedFailure, NotFoundFailure404}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.time._
import utils.Slick.implicits._
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
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(
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
      val future = Rmas.save(Factories.rma.copy(
        referenceNumber = s"RMA-$i",
        orderId = currentOrder.id,
        orderRefNum = currentOrder.referenceNumber,
        customerId = Some(currentCustomer.id))
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

      // TODO: Remove pending when `selectOneWithMetaData` will be merged
      "should return failure for non-existing customer" in new Fixture {
        pending
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

      // TODO: Remove pending when `selectOneWithMetaData` will be merged
      "should return failure for non-existing order" in new Fixture {
        pending
        val response = GET(s"v1/rmas/order/ABC-666")
        response.status must ===(StatusCodes.NotFound)
        response.errors must ===(NotFoundFailure404(Order, "ABC-666").description)
      }
    }

    "GET /v1/rmas/:code" - {
      "should return valid RMA by code" in new Fixture {
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

    "POST /v1/rmas/:refNum/lock" - {
      "successfully locks an RMA" in {
        Orders.save(Factories.order).run().futureValue
        val rma = Rmas.save(Factories.rma).run().futureValue
        StoreAdmins.save(Factories.storeAdmin).run().futureValue

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
        Orders.save(Factories.order).run().futureValue
        val rma = Rmas.save(Factories.rma.copy(locked = true)).run().futureValue

        val response = POST(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.BadRequest)
        response.errors must === (LockedFailure(Rma, rma.referenceNumber).description)
      }

      "avoids race condition" in {
        StoreAdmins.save(Factories.storeAdmin).run().futureValue
        Orders.save(Factories.order).run().futureValue
        val rma = Rmas.save(Factories.rma).run().futureValue

        def request = POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val responses = Seq(0, 1).par.map(_ ⇒ request)
        responses.map(_.status) must contain allOf(StatusCodes.OK, StatusCodes.BadRequest)
        RmaLockEvents.result.run().futureValue.length mustBe 1
      }
    }

    "POST /v1/rmas/:refNum/unlock" - {
      "unlocks an RMA" in {
        StoreAdmins.save(Factories.storeAdmin).run().futureValue
        Orders.save(Factories.order).run().futureValue
        val rma = Rmas.save(Factories.rma).run().futureValue

        POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")
        response.status must === (StatusCodes.OK)

        val unlockedRma = Rmas.findByRefNum(rma.referenceNumber).result.run().futureValue.head
        unlockedRma.locked must === (false)
      }

      "refuses to unlock an already unlocked RMA" in {
        Orders.save(Factories.order).run().futureValue
        val rma = Rmas.save(Factories.rma).run().futureValue
        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")

        response.status must === (StatusCodes.BadRequest)
        response.errors must === (GeneralFailure("Return is not locked").description)
      }
    }
  }

  trait Fixture {
    val (customer, order, rma) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(
        status = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
      rma ← Rmas.save(Factories.rma.copy(
        orderId = order.id,
        orderRefNum = order.referenceNumber,
        customerId = Some(customer.id)))
    } yield (customer, order, rma)).run().futureValue
  }

}
