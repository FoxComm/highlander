package models

import models.Order._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class OrdersTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Orders" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "")).futureValue

      order.referenceNumber must === ("BR10001")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "R123456")).futureValue
      order.referenceNumber must === ("R123456")
    }

    "can only have one record in 'cart' status" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id)).futureValue

      val failure = Orders.create(order.copy(id = 0)).failed.futureValue
      failure.getMessage must include( """value violates unique constraint "orders_has_only_one_cart"""")
    }

    "trigger resets remorse period after status changes from RemorseHold" in {
      val order = Orders.save(Factories.order.copy(remorsePeriodInMinutes = 11, status = RemorseHold)).run().futureValue

      db.run(Orders.findByRefNum(order.referenceNumber).map(_.status).update(ManualHold)).futureValue

      val updated = db.run(Orders.findByRefNum(order.referenceNumber).result).futureValue.head
      updated.remorsePeriodInMinutes must ===(30)
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }
}
