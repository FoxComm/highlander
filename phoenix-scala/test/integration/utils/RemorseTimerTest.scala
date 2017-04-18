package utils

import java.time.Instant

import akka.pattern.ask
import akka.testkit.TestActorRef
import cats.implicits._
import models.cord._
import services.actors._
import testutils._
import testutils.fixtures.BakedFixtures

class RemorseTimerTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  val timer: TestActorRef[RemorseTimer] = TestActorRef(new RemorseTimer())

  "Remorse timer" - {

    "advances to fulfillment once remorse period ends" in new Order_Baked {
      val overdue = Some(Instant.now.minusSeconds(60))
      Orders.update(order, order.copy(remorsePeriodEnd = overdue)).gimme
      tick()
      val updated = Orders.refresh(order).gimme
      updated.remorsePeriodEnd must === (None)
      updated.state must === (Order.FulfillmentStarted)
    }
  }

  def tick(): Unit = {
    // Response received
    (timer ? Tick).futureValue match {
      case r: RemorseTimerResponse ⇒
        // TODO: get rid of explicit runEmptyA @michalrus
        r.updatedQuantity.runEmptyA.value.futureValue // Response contains future, so wait on that
      case _ ⇒
        fail("Remorse timer had to reply with Future but something went wrong")
    }
  }
}
