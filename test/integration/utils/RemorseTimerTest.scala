package utils

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}

import models.cord._
import models.customer.Customers
import org.scalatest.BeforeAndAfterAll
import services.actors._
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class RemorseTimerTest(_system: ActorSystem)
    extends TestKit(_system)
    with IntegrationTestBase
    with BeforeAndAfterAll
    with TestObjectContext {

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
    val order = (for {
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
      order    ← * <~ Orders.create(cart.toOrder())
    } yield order).gimme
  }
}
