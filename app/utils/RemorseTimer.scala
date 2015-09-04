package utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import akka.actor.{Actor, ActorLogging}

import models.Order._
import models.{Order, Orders}
import slick.driver.PostgresDriver.api._

case object Tick

class RemorseTimer(implicit db: Database) extends Actor {
  import context.dispatcher

  override def receive = {
    case Tick ⇒ sender() ! tick
  }

  private def tick: Future[Unit] = {
    val orders = Orders
      .filter(_.status === (RemorseHold: Status))
      .filterNot(_.locked)

    val advance = db.run(orders
      .filterNot(_.remorsePeriodInMinutes > 0)
      .map(_.status)
      .update(Order.FulfillmentStarted)
      .transactionally)

    val decrement = db.stream(orders
      .filter(_.remorsePeriodInMinutes > 0)
      .mutate.transactionally)
      .foreach(mutate ⇒ mutate.row = mutate.row.copy(remorsePeriodInMinutes = mutate.row.remorsePeriodInMinutes - 1))

    for {
      _ ← advance
      _ ← decrement
    } yield ()
  }
}

/*
Dummy actor RemorseTimer can talk to.
RemorseTimer replies with (currently empty) future, so this may be extended to log remorse timer activity or something.
 */
class RemorseTimerMate(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  override def receive = {
    case response: Future[_] ⇒ response.onComplete {
      case Success(_) ⇒ // log.debug("Remorse timer completed successfully")
      case _ ⇒ log.error("Remorse timer failed")
    }
    case _ ⇒ log.error("Remorse timer is in trouble, it can't communicate properly :(")
  }
}
