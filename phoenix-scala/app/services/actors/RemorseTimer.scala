package services.actors

import java.time.Instant

import scala.util.Success

import akka.actor.{Actor, ActorLogging}
import models.activity.ActivityContext
import models.cord.Order._
import models.cord.{Order, Orders}
import services.LogActivity
import services.Result
import utils.aliases._
import utils.db.javaTimeSlickMapper
import utils.db.ExPostgresDriver.api._
import utils.db._

case object Tick

case class RemorseTimerResponse(updatedQuantity: Result[Int])

class RemorseTimer(implicit db: DB, ec: EC) extends Actor {
  implicit val ac = ActivityContext.build(userId = 1, userType = "admin")

  override def receive = {
    case Tick ⇒ sender() ! tick
  }

  private def tick(implicit db: DB): RemorseTimerResponse = {
    val orders = Orders
      .filter(_.state === (RemorseHold: State))
      .filter(_.remorsePeriodEnd.map(_ < Instant.now))

    val newState = Order.FulfillmentStarted

    val query = for {
      cordRefs ← * <~ orders.result
      count    ← * <~ orders.map(_.state).update(newState)
      _        ← * <~ LogActivity.orderBulkStateChanged(newState, cordRefs.map(_.referenceNumber))
    } yield count

    RemorseTimerResponse(query.runTxn)
  }
}

/*
 * Dummy actor RemorseTimer can talk to.
 * RemorseTimer replies with (currently empty) future,
 * so this may be extended to log remorse timer activity or something.
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
