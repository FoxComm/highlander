package models

import java.time.Instant.now

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.cord.Order._
import models.cord._
import models.customer.Customers
import util._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time._

class OrdersIntegrationTest extends IntegrationTestBase with TestObjectContext {

  "Orders" - {

    "trigger sets sets/resets period end when order moves to/from RemorseHold" in new Fixture {
      order.remorsePeriodEnd mustBe defined
      val updated1 = Orders.updateReturning(order, order.copy(state = ManualHold)).gimme
      updated1.remorsePeriodEnd must not be defined
      val updated2 = Orders.updateReturning(order, order.copy(state = RemorseHold)).gimme
      updated2.remorsePeriodEnd.value.minuteOfHour must === (now.plusMinutes(30).minuteOfHour)
    }

    "trigger does not change remorse period end if defined when order moves to RemorseHold" in new Fixture {
      val newRemorseEnd = now.plusMinutes(15)
      val withRemorse   = order.copy(state = RemorseHold, remorsePeriodEnd = newRemorseEnd.some)
      val updated       = Orders.updateReturning(order, withRemorse).gimme
      updated.remorsePeriodEnd.value.minuteOfHour must === (newRemorseEnd.minuteOfHour)
    }

    "remorse period end is generated if empty" in new Fixture {
      val updated = Orders.updateReturning(order, order.copy(remorsePeriodEnd = None)).gimme
      updated.remorsePeriodEnd.value.minuteOfHour must === (now.plusMinutes(30).minuteOfHour)
    }
  }

  trait Fixture {
    val order = (for {
      _     ← * <~ Customers.create(Factories.customer)
      cart  ← * <~ Carts.create(Factories.cart)
      order ← * <~ Orders.create(cart.toOrder())
    } yield order).gimme
  }
}
