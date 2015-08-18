package utils

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout

import models.Order.FulfillmentStarted
import models.{Order, Orders}
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import slick.driver.PostgresDriver.api._
import util.DbTestSupport
import utils.Seeds.Factories

class RemorseTimerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll with DbTestSupport {

  def this() = this(ActorSystem("RemorseTimerTest"))

  val timer = TestActorRef(new RemorseTimer())

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout: Timeout = testKitSettings.DefaultTimeout

  def fetchOrder = db.run(Orders.findByRefNum("ABCD1234-11").result).futureValue.head

  "Remorse timer" must {
    "decrement remorse period once a tick" in new Fixture {
      (timer ? Tick).onSuccess { case _ ⇒ fetchOrder.remorsePeriodInMinutes must ===(29) }

      timer ! Tick
      timer ! Tick
      (timer ? Tick).onSuccess { case _ ⇒ fetchOrder.remorsePeriodInMinutes must ===(26) }
    }

    "pause timer while order is locked" in new Fixture {
      Orders.save(order.copy(locked = true)).run().futureValue
      (timer ? Tick).onSuccess { case _ ⇒ fetchOrder.remorsePeriodInMinutes must ===(30) }

      Orders.save(order.copy(locked = false)).run().futureValue
      (timer ? Tick).onSuccess { case _ ⇒ fetchOrder.remorsePeriodInMinutes must ===(29) }
    }

    "advance to fulfillment once remorse period ends" in new Fixture {
      Orders.save(order.copy(remorsePeriodInMinutes = 1)).run().futureValue
      (timer ? Tick).onSuccess { case _ ⇒ fetchOrder.status must ===(FulfillmentStarted) }
    }
  }

  trait Fixture {
    val order = Orders.save(Factories.order.copy(
      status = Order.RemorseHold,
      remorsePeriodInMinutes = 30)).run().futureValue
  }

}
