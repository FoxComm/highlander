package services.actors

import java.time.Instant

import scala.concurrent.Future
import scala.util.Success
import akka.actor.{Actor, ActorLogging}
import models.cord.Order._
import models.cord.{Order, Orders}
import services.LogActivity
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db.javaTimeSlickMapper

case object Tick

case class RemorseTimerResponse(updatedQuantity: Future[Int])

class RemorseTimer(implicit db: DB) extends Actor {
  override def receive = {
    case Tick ⇒ sender() ! tick
  }

  private def tick(implicit db: DB): RemorseTimerResponse = {
    val orders = Orders.filter(_.state === (RemorseHold: State)).filter(_.remorsePeriodEnd.map(_ < Instant.now))

    val query = for {
      cordRefs ← orders.result
      count    ← orders.map(_.state).update(Order.FulfillmentStarted)
      _        ← LogActivity.orderBulkStateChanged(Order.FulfillmentStarted, cordRefs.map(_.referenceNumber))
    } yield count

    RemorseTimerResponse(db.run(query))
  }
}

/*
Dummy actor RemorseTimer can talk to.
RemorseTimer replies with (currently empty) future, so this may be extended to log remorse timer activity or something.
 */
class RemorseTimerMate(implicit ec: EC) extends Actor with ActorLogging {

  override def receive = {
    case response: RemorseTimerResponse ⇒
      response.updatedQuantity.onComplete {
        case Success(quantity) ⇒ log.debug(s"Remorse timer updated $quantity orders")
        case _                 ⇒ log.error("Remorse timer failed")
      }
    case _ ⇒ log.error("Remorse timer is in trouble, it can't communicate properly :(")
  }
}
