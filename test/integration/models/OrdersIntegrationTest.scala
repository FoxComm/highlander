package models

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import models.cord.Order._
import models.cord.Orders
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.seeds.Seeds.Factories
import utils.time._

class OrdersIntegrationTest extends IntegrationTestBase {

  "Orders" - {

    "trigger sets remorse period end when order moves to RemorseHold" in {
      val order = Orders.create(Factories.order).gimme

      order.remorsePeriodEnd must === (None)

      Orders.update(order, order.copy(state = RemorseHold)).gimme

      val updatedOrder = Orders.findByRefNum(order.refNum).gimme.headOption.value
      updatedOrder.remorsePeriodEnd.value.minuteOfHour must === (
          Instant.now.plusMinutes(30).minuteOfHour)
    }

    "trigger resets remorse period after status changes from RemorseHold" in {
      val order = Orders
        .create(Factories.order.copy(remorsePeriodEnd = Some(Instant.now), state = RemorseHold))
        .gimme

      Orders.findByRefNum(order.refNum).map(_.state).update(ManualHold).gimme

      val updated = Orders.findByRefNum(order.refNum).result.gimme.headOption.value
      updated.remorsePeriodEnd must === (None)
    }

  }
}
