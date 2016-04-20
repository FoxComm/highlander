package models

import java.time.Instant

import models.order.{Orders, Order}
import Order._
import models.customer.Customers
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.seeds.Seeds
import Seeds.Factories
import utils.db._
import utils.time._

class OrdersIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Orders" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "")).run().futureValue.rightVal

      order.referenceNumber must === ("BR10001")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "R123456")).run()
        .futureValue.rightVal
      order.referenceNumber must === ("R123456")
    }

    "can only have one record in 'cart' status" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id)).run().futureValue.rightVal

      val failure = Orders.create(order.copy(id = 0, referenceNumber = order.refNum + "ZZZ")).run().futureValue.leftVal
      failure.getMessage must include("""value violates unique constraint "orders_has_only_one_cart"""")
    }

    "has a unique index on referenceNumber" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id)).run().futureValue.rightVal

      val failure = Orders.create(order.copy(id = 0).copy(state = RemorseHold)).run().futureValue.leftVal
      failure.getMessage must include("""value violates unique constraint "orders_reference_number_key"""")
    }

    "trigger sets remorse period end when order moves to RemorseHold" in {
      val order = Orders.create(Factories.order).run().futureValue.rightVal

      order.remorsePeriodEnd must ===(None)

      db.run(Orders.update(order, order.copy(state = RemorseHold))).futureValue mustBe 'right

      val updatedOrder = Orders.findByRefNum(order.referenceNumber).result.run().futureValue.headOption.value
      updatedOrder.remorsePeriodEnd.value.minuteOfHour must === (Instant.now.plusMinutes(30).minuteOfHour)
    }

    "trigger resets remorse period after status changes from RemorseHold" in {
      val order = Orders.create(Factories.order.copy(
        remorsePeriodEnd = Some(Instant.now),
        state = RemorseHold))
        .run().futureValue.rightVal

      db.run(Orders.findByRefNum(order.referenceNumber).map(_.state).update(ManualHold)).futureValue

      val updated = db.run(Orders.findByRefNum(order.referenceNumber).result).futureValue.headOption.value
      updated.remorsePeriodEnd must ===(None)
    }
  }

  trait Fixture {
    val customer = Customers.create(Factories.customer).run().futureValue.rightVal
  }
}
