import java.time.Instant

import Extensions._
import akka.http.scaladsl.model.StatusCodes

import cats.data.Xor
import cats.implicits._
import models.order.{Order, Orders}
import Order._
import models.customer.Customers
import models.payment.creditcard.CreditCardCharge
import responses.BatchResponse
import responses.order._
import services.orders.OrderQueries
import util.IntegrationTestBase
import utils.seeds.Seeds.Factories
import utils.seeds.RankingSeedsGenerator
import utils.db._
import utils.db.DbResultT._
import utils.time._
import scala.concurrent.ExecutionContext.Implicits.global

import failures.LockFailures.LockedFailure
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import payloads.OrderPayloads.BulkUpdateOrdersPayload

class AllOrdersIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with SortingAndPaging[AllOrders.Root]
    with AutomaticAuth {

  // paging and sorting API
  def uriPrefix = "v1/orders"

  def responseItems = {
    val dbio = for {
      customer ← * <~ Customers.create(RankingSeedsGenerator.generateCustomer)
      insertOrders = (1 to numOfResults).map { _ ⇒
        Factories.order.copy(customerId = customer.id,
                             referenceNumber =
                               RankingSeedsGenerator.randomString(10),
                             state = Order.RemorseHold,
                             remorsePeriodEnd =
                               Some(Instant.now.plusMinutes(30)))
      }

      _ ← * <~ Orders.createAll(insertOrders)
    } yield ()

    dbio.runTxn().futureValue
    getAllOrders.toIndexedSeq
  }

  val sortColumnName = "referenceNumber"

  def responseItemsSort(items: IndexedSeq[AllOrders.Root]) =
    items.sortBy(_.referenceNumber)

  def mf = implicitly[scala.reflect.Manifest[AllOrders.Root]]
  // paging and sorting API end

  def getAllOrders: Seq[AllOrders.Root] = {
    OrderQueries.list.futureValue match {
      case Xor.Left(s) ⇒ fail(s.toList.mkString(";"))
      case Xor.Right(seq) ⇒ seq.result
    }
  }

  "GET /v1/orders" - {
    "find all" in {
      val cId =
        Customers.create(Factories.customer).run().futureValue.rightVal.id
      Orders
        .create(Factories.order.copy(customerId = cId))
        .run()
        .futureValue
        .rightVal

      val responseJson = GET(s"v1/orders")
      responseJson.status must ===(StatusCodes.OK)

      val allOrders = responseJson.ignoreFailuresAndGiveMe[Seq[AllOrders.Root]]
      allOrders.size must ===(1)

      val actual = allOrders.head

      val expected = AllOrders.Root(referenceNumber = "ABCD1234-11",
                                    name = "Yax Fuentes".some,
                                    email = "yax@yax.com".some,
                                    orderState = Order.ManualHold,
                                    paymentState = CreditCardCharge.Cart.some,
                                    shippingState = Order.ManualHold.some,
                                    placedAt = None,
                                    total = 0,
                                    remorsePeriodEnd = None)

      actual must ===(expected)
    }
  }

  "PATCH /v1/orders" - {
    "bulk update states" in new StateUpdateFixture {
      val response =
        PATCH("v1/orders",
              BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"),
                                      FulfillmentStarted))

      response.status must ===(StatusCodes.OK)

      val all = response.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must contain allOf (
          ("foo", FulfillmentStarted),
          ("bar", RemorseHold)
      )

      all.errors.value must contain allOf (LockedFailure(Order, "bar").description,
          NotFoundFailure404(Order, "nonExistent").description)
    }

    "refuses invalid status transition" in {
      val customer =
        Customers.create(Factories.customer).run().futureValue.rightVal
      val order = Orders
        .create(Factories.order.copy(customerId = customer.id))
        .run()
        .futureValue
        .rightVal
      val response =
        PATCH("v1/orders", BulkUpdateOrdersPayload(Seq(order.refNum), Cart))

      response.status must ===(StatusCodes.OK)
      val all = response.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must ===(Seq((order.refNum, order.state)))

      all.errors.value.head must ===(StateTransitionNotAllowed(
              order.state, Cart, order.refNum).description)
    }

    "bulk update states with paging and sorting" in new StateUpdateFixture {
      val responseJson = PATCH(
          "v1/orders?size=2&from=1&sortBy=referenceNumber",
          BulkUpdateOrdersPayload(Seq("foo", "bar", "nonExistent"),
                                  FulfillmentStarted)
      )

      responseJson.status must ===(StatusCodes.OK)

      val all = responseJson.as[BatchResponse[AllOrders.Root]]
      val allOrders = all.result.map(o ⇒ (o.referenceNumber, o.orderState))

      allOrders must contain theSameElementsInOrderAs Seq(
          ("foo", FulfillmentStarted)
      )

      all.errors.value must contain allOf (LockedFailure(Order, "bar").description,
          NotFoundFailure404(Order, "nonExistent").description)
    }
  }

  trait StateUpdateFixture {
    (for {
      cust ← * <~ Customers.create(Factories.customer)
      foo ← * <~ Orders.create(Factories.order.copy(customerId = cust.id,
                                                    referenceNumber = "foo",
                                                    state = FraudHold))
      bar ← * <~ Orders.create(Factories.order.copy(customerId = cust.id,
                                                    referenceNumber = "bar",
                                                    state = RemorseHold,
                                                    isLocked = true))
      baz ← * <~ Orders.create(Factories.order.copy(customerId = cust.id,
                                                    referenceNumber = "baz",
                                                    state = ManualHold))
    } yield (cust, foo, bar)).runTxn().futureValue.rightVal
  }
}
