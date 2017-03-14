package services.actors

import cats.implicits._
import java.time.Instant
import faker.Lorem.letterify

import scala.util.Success
import akka.actor.{Actor, ActorLogging}
import models.activity.ActivityContext
import models.cord.Order._
import models.cord.{Order, Orders}
import services.LogActivity
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case object Tick

// FIXME: what is this, Result has Future inside! @michalrus
case class RemorseTimerResponse(updatedQuantity: Result[Int])

class RemorseTimer(implicit db: DB, ec: EC) extends Actor {

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
      _        ← * <~ doOrMeh(count > 0, logAcitvity(newState, cordRefs))
    } yield count

    RemorseTimerResponse(query.runTxn)
  }

  private def logAcitvity(newState: Order.State, orders: Seq[Order])(
      implicit ec: EC): DbResultT[Unit] =
    DbResultT
      .seqCollectFailures(
          orders
            .groupBy(_.scope)
            .map {
          case (scope, scopeOrders) ⇒
            val refNums = scopeOrders.map(_.referenceNumber)

            implicit val ac = ActivityContext.build(userId = 1,
                                                    userType = "admin",
                                                    scope = scope,
                                                    transactionId = letterify("?" * 5))

            LogActivity().withScope(scope).orderBulkStateChanged(newState, refNums)
        }
            .toList)
      .meh
}

/*
 * Dummy actor RemorseTimer can talk to.
 * RemorseTimer replies with (currently empty) future,
 * so this may be extended to log remorse timer activity or something.
 */
class RemorseTimerMate(implicit ec: EC) extends Actor with ActorLogging {

  override def receive = {
    case response: RemorseTimerResponse ⇒
      response.updatedQuantity.runEmptyA.value.onComplete {
        // TODO: do we now quantity is `Failures Xor Int` here? @michalrus
        case Success(quantity) ⇒ log.debug(s"Remorse timer updated $quantity orders")
        case _                 ⇒ log.error("Remorse timer failed")
      }
    case _ ⇒ log.error("Remorse timer is in trouble, it can't communicate properly :(")
  }
}
