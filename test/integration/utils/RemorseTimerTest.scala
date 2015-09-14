package utils

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}

import models.Order.FulfillmentStarted
import models.{OrderLockEvents, StoreAdmins, Order, Orders}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import services.OrderUpdater
import slick.driver.PostgresDriver.api._
import util.{DbTestSupport, IntegrationTestBase}
import utils.Seeds.Factories
import utils.Slick.implicits._

import utils.time.JavaTimeSlickMapper._

class RemorseTimerTest(_system: ActorSystem) extends TestKit(_system) with IntegrationTestBase with BeforeAndAfterAll {

  def this() = this(ActorSystem("RemorseTimerTest"))

  override def afterAll() = TestKit.shutdownActorSystem(system)

  val timer = TestActorRef(new RemorseTimer())

  def byRefNum = Orders.findByRefNum("ABCD1234-11")

  def updated = byRefNum.one.run().futureValue.get

  "Remorse timer" - {

    "advances to fulfillment once remorse period ends" in new Fixture {
      val overdue = Option(Instant.now.minusSeconds(60))
      byRefNum.map(_.remorsePeriodEnd).update(overdue).run().futureValue
      tick()
      updated.remorsePeriodEnd must ===(None)
      updated.status must ===(FulfillmentStarted)
    }
  }

  def tick(): Unit = {
    // Response received
    (timer ? Tick).futureValue match {
      case r: RemorseTimerResponse ⇒ r.updatedQuantity.futureValue // Response contains future, so wait on that
      case _ ⇒ fail("Remorse timer had to reply with Future but something went wrong")
    }
  }

  trait Fixture {
    val order = Orders.save(Factories.order.copy(
      status = Order.RemorseHold,
      remorsePeriodEnd = Some(Instant.now.plusSeconds(30 * 60))))
      .run().futureValue
  }
}
