package models

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customers
import models.order.Order._
import models.order.Orders
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time._

class OrdersIntegrationTest extends IntegrationTestBase {

  "Orders" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val order =
        Orders.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "")).gimme

      order.referenceNumber must === ("BR10001")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val order = Orders
        .create(Factories.cart.copy(customerId = customer.id, referenceNumber = "R123456"))
        .gimme
      order.referenceNumber must === ("R123456")
    }

    "can only have one record in 'cart' status" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id)).gimme

      val failure = Orders
        .create(order.copy(id = 0, referenceNumber = order.refNum + "ZZZ"))
        .run()
        .futureValue
        .leftVal
      failure.getMessage must include(
          """value violates unique constraint "orders_has_only_one_cart"""")
    }

    "has a unique index on referenceNumber" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id)).gimme

      val failure =
        Orders.create(order.copy(id = 0).copy(state = RemorseHold)).run().futureValue.leftVal
      failure.getMessage must include(
          """value violates unique constraint "orders_reference_number_key"""")
    }

    "trigger sets remorse period end when order moves to RemorseHold" in {
      val order = Orders.create(Factories.order).gimme

      order.remorsePeriodEnd must === (None)

      Orders.update(order, order.copy(state = RemorseHold)).run().futureValue mustBe 'right

      val updatedOrder = Orders.findByRefNum(order.referenceNumber).gimme.headOption.value
      updatedOrder.remorsePeriodEnd.value.minuteOfHour must === (
          Instant.now.plusMinutes(30).minuteOfHour)
    }

    "trigger resets remorse period after status changes from RemorseHold" in {
      val order = Orders
        .create(Factories.order.copy(remorsePeriodEnd = Some(Instant.now), state = RemorseHold))
        .gimme

      db.run(Orders.findByRefNum(order.referenceNumber).map(_.state).update(ManualHold))
        .futureValue

      val updated =
        db.run(Orders.findByRefNum(order.referenceNumber).result).futureValue.headOption.value
      updated.remorsePeriodEnd must === (None)
    }
  }

  trait Fixture {
    val customer = Customers.create(Factories.customer).gimme
  }
}
