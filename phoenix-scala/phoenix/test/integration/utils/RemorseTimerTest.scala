package utils

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}
import cats.implicits._
import java.time.Instant
import org.scalatest.BeforeAndAfterAll
import phoenix.models.cord._
import phoenix.services.actors._
import testutils._
import testutils.fixtures.BakedFixtures

class RemorseTimerTest(_system: ActorSystem)
    extends TestKit(_system)
    with IntegrationTestBase
    with BeforeAndAfterAll
    with TestObjectContext
    with BakedFixtures {

  def this() = this(ActorSystem("RemorseTimerTest"))

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  val timer: TestActorRef[RemorseTimer] = TestActorRef(new RemorseTimer())(implicitly, system)

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

  def tick(): Unit =
    // Response received
    (timer ? Tick).futureValue match {
      case r: RemorseTimerResponse ⇒
        // TODO: get rid of explicit runEmptyA @michalrus
        r.updatedQuantity.runEmptyA.value.futureValue // Response contains future, so wait on that
      case _ ⇒
        fail("Remorse timer had to reply with Future but something went wrong")
    }
}
