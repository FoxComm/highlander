package utils

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}

import models.cord.{Order, Orders}
import org.scalatest.BeforeAndAfterAll
import services.actors._
import util.IntegrationTestBase
import utils.seeds.Seeds.Factories

class RemorseTimerTest(_system: ActorSystem)
    extends TestKit(_system)
    with IntegrationTestBase
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("RemorseTimerTest"))

  override def afterAll() = TestKit.shutdownActorSystem(system)

  val timer = TestActorRef(new RemorseTimer())

  "Remorse timer" - {

    "advances to fulfillment once remorse period ends" in new Fixture {
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
        r.updatedQuantity.futureValue // Response contains future, so wait on that
      case _ ⇒
        fail("Remorse timer had to reply with Future but something went wrong")
    }
  }

  trait Fixture {
    val order = Orders
      .create(Factories.order.copy(state = Order.RemorseHold,
                                   remorsePeriodEnd = Some(Instant.now.plusSeconds(30 * 60))))
      .gimme
  }
}
