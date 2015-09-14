package utils

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import akka.actor.{Actor, ActorLogging}

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.Order._
import models.{Order, Orders}
import slick.driver.PostgresDriver.api._

import time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

case object Tick

final case class RemorseTimerResponse(updatedQuantity: Future[Int])

class RemorseTimer(implicit ec: ExecutionContext, db: Database) extends Actor {
  override def receive = {
    case Tick ⇒ sender() ! tick
  }

  private def tick(implicit ec: ExecutionContext, db: Database): RemorseTimerResponse = {
    val advance = Orders
      .filter(_.status === (RemorseHold: Status))
      .filterNot(_.locked)
      .filter(_.remorsePeriodEnd.map(_ < Instant.now))
      .map(_.status)
      .update(Order.FulfillmentStarted)

    RemorseTimerResponse(db.run(advance))
  }
}

/*
Dummy actor RemorseTimer can talk to.
RemorseTimer replies with (currently empty) future, so this may be extended to log remorse timer activity or something.
 */
class RemorseTimerMate(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  override def receive = {
    case response: RemorseTimerResponse ⇒ response.updatedQuantity.onComplete {
      case Success(quantity) ⇒ log.debug(s"Remorse timer updated $quantity orders")
      case _ ⇒ log.error("Remorse timer failed")
    }
    case _ ⇒ log.error("Remorse timer is in trouble, it can't communicate properly :(")
  }
}
