package utils

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}

import models.order.Order.FulfillmentStarted
import models.order.{Order, Orders}
import org.scalatest.BeforeAndAfterAll
import services.actors._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class RemorseTimerTest(_system: ActorSystem)
    extends TestKit(_system)
    with IntegrationTestBase
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("RemorseTimerTest"))

  override def afterAll() = TestKit.shutdownActorSystem(system)

  val timer = TestActorRef(new RemorseTimer())

  def byRefNum = Orders.findByRefNum("ABCD1234-11")

  def updated = byRefNum.one.run().futureValue.value

  "Remorse timer" - {

    "advances to fulfillment once remorse period ends" in new Fixture {
      val overdue = Option(Instant.now.minusSeconds(60))
      byRefNum.map(_.remorsePeriodEnd).update(overdue).run().futureValue
      tick()
      updated.remorsePeriodEnd must ===(None)
      updated.state must ===(FulfillmentStarted)
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
