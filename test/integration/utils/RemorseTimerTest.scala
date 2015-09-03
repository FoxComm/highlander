package utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}

import models.Order.FulfillmentStarted
import models.{Order, Orders}
import org.scalatest.BeforeAndAfterAll
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class RemorseTimerTest(_system: ActorSystem) extends TestKit(_system) with IntegrationTestBase with BeforeAndAfterAll {

  def this() = this(ActorSystem("RemorseTimerTest"))

  override def afterAll() = TestKit.shutdownActorSystem(system)

  val timer = TestActorRef(new RemorseTimer())

  def byRefNum = Orders.findByRefNum("ABCD1234-11")

  def updated = byRefNum.one.run().futureValue.get

  "Remorse timer" - {
    "decrement remorse period once a tick" in new Fixture {
      tick()
      updated.remorsePeriodInMinutes must ===(29)

      tick()
      tick()
      tick()
      updated.remorsePeriodInMinutes must ===(26)
    }

    "pause timer while order is locked" in new Fixture {
      byRefNum.map(_.locked).update(true).run().futureValue
      tick()
      updated.remorsePeriodInMinutes must ===(30)

      byRefNum.map(_.locked).update(false).run().futureValue
      tick()
      updated.remorsePeriodInMinutes must ===(29)
    }

    "advance to fulfillment once remorse period ends" in new Fixture {
      byRefNum.map(_.remorsePeriodInMinutes).update(1).run().futureValue
      tick() // Drops remorse period to zero
      tick() // Advances
      updated.status must ===(FulfillmentStarted)
    }
  }

  def tick(): Unit = {
    // Response received
    (timer ? Tick).futureValue match {
      case r: Future[_] ⇒ r.futureValue // Response contains future, so wait on that
      case _ ⇒ fail("Remorse timer had to reply with Future but something went wrong")
    }
  }

  trait Fixture {
    val order = Orders.save(Factories.order.copy(
      status = Order.RemorseHold,
      remorsePeriodInMinutes = 30)).run().futureValue
  }

}
