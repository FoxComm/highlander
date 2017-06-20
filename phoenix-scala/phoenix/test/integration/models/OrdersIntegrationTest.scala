package models

import java.time.Instant.now

import cats.implicits._
import phoenix.models.cord.Order._
import phoenix.models.cord._
import phoenix.utils.time._
import testutils._
import testutils.fixtures.BakedFixtures

class OrdersIntegrationTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "Orders" - {

    "trigger sets sets/resets period end when order moves to/from RemorseHold" in new Order_Baked {
      order.remorsePeriodEnd mustBe defined
      val updated1 = Orders.updateReturning(order, order.copy(state = ManualHold)).gimme
      updated1.remorsePeriodEnd must not be defined
      val updated2 = Orders.updateReturning(order, order.copy(state = RemorseHold)).gimme
      updated2.remorsePeriodEnd.value.minuteOfHour must === (now.plusMinutes(30).minuteOfHour)
    }

    "trigger does not change remorse period end if defined when order moves to RemorseHold" in new Order_Baked {
      val newRemorseEnd = now.plusMinutes(15)
      val withRemorse   = order.copy(state = RemorseHold, remorsePeriodEnd = newRemorseEnd.some)
      val updated       = Orders.updateReturning(order, withRemorse).gimme
      updated.remorsePeriodEnd.value.minuteOfHour must === (newRemorseEnd.minuteOfHour)
    }

    "remorse period end is generated if empty" in new Order_Baked {
      val updated = Orders.updateReturning(order, order.copy(remorsePeriodEnd = None)).gimme
      updated.remorsePeriodEnd.value.minuteOfHour must === (now.plusMinutes(30).minuteOfHour)
    }
  }
}
