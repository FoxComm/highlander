package models

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.cord.Order._
import models.cord._
import util._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time._

class OrdersIntegrationTest extends IntegrationTestBase with TestObjectContext {

  "Orders" - {

    "trigger sets remorse period end when order moves to RemorseHold" in {
      val order = (for {
        cart    ← * <~ Carts.create(Factories.cart)
        order   ← * <~ Orders.create(cart.toOrder().copy(state = ManualHold))
        updated ← * <~ Orders.updateReturning(order, order.copy(state = RemorseHold))
      } yield updated).gimme
      order.remorsePeriodEnd.value.minuteOfHour must === (Instant.now.plusMinutes(30).minuteOfHour)
    }

    "trigger does not change remorse period end if defined when order moves to RemorseHold" in {
      val order = (for {
        cart  ← * <~ Carts.create(Factories.cart)
        order ← * <~ Orders.create(cart.toOrder().copy(state = ManualHold))
        updated ← * <~ Orders.updateReturning(order,
                                              order.copy(state = RemorseHold,
                                                         remorsePeriodEnd =
                                                           Instant.now.plusMinutes(15).some))
      } yield updated).gimme
      order.remorsePeriodEnd.value.minuteOfHour must === (Instant.now.plusMinutes(15).minuteOfHour)
    }

    "trigger resets remorse period after status changes from RemorseHold" in {
      val cart  = Carts.create(Factories.cart).gimme
      val order = Orders.create(cart.toOrder()).gimme

      val updated = Orders.updateReturning(order, order.copy(state = ManualHold)).gimme
      updated.remorsePeriodEnd must not be defined
    }

    "remorse period end is generated if empty" in {
      val cart  = Carts.create(Factories.cart).gimme
      val order = Orders.create(cart.toOrder().copy(remorsePeriodEnd = None)).gimme
      order.remorsePeriodEnd.value.minuteOfHour must === (Instant.now.plusMinutes(30).minuteOfHour)
    }
  }
}
