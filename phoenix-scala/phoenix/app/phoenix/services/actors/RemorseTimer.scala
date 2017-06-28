package phoenix.services.actors

import akka.actor.{Actor, ActorLogging}
import cats.implicits._
import core.db.ExPostgresDriver.api._
import core.db._
import faker.Lorem.letterify
import java.time.Instant
import phoenix.models.activity.{ActivityContext, EnrichedActivityContext}
import phoenix.models.cord.Order._
import phoenix.models.cord.{Order, Orders}
import phoenix.services.LogActivity
import phoenix.utils.apis.Apis
import scala.util.Success

case object Tick

// FIXME: what is this, Result has Future inside! @michalrus
case class RemorseTimerResponse(updatedQuantity: Result[Int])

class RemorseTimer(implicit db: DB, ec: EC, apis: Apis) extends Actor {

  override def receive = {
    case Tick ⇒ sender() ! tick
  }

  private def tick: RemorseTimerResponse = {
    val orders = Orders
      .filter(_.state === (RemorseHold: State))
      .filter(_.remorsePeriodEnd.map(_ < Instant.now))

    val newState = Order.FulfillmentStarted

    val query = for {
      cordRefs ← * <~ orders.result
      count    ← * <~ orders.map(_.state).update(newState)
      _        ← * <~ when(count > 0, logAcitvity(newState, cordRefs))
    } yield count

    RemorseTimerResponse(query.runTxn)
  }

  private def logAcitvity(newState: Order.State, orders: Seq[Order]): DbResultT[Unit] =
    DbResultT
      .seqCollectFailures(
        orders
          .groupBy(_.scope)
          .map {
            case (scope, scopeOrders) ⇒
              val refNums = scopeOrders.map(_.referenceNumber)

              implicit val ac = EnrichedActivityContext(ctx = ActivityContext(userId = 1,
                                                                              userType = "admin",
                                                                              scope = scope,
                                                                              transactionId =
                                                                                letterify("?" * 5)),
                                                        producer = apis.kafka)

              LogActivity().withScope(scope).orderBulkStateChanged(newState, refNums)
          }
          .toList)
      .void
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
        // TODO: do we now quantity is `Either[Failures, Int]` here? @michalrus
        case Success(quantity) ⇒ log.debug(s"Remorse timer updated $quantity orders")
        case _                 ⇒ log.error("Remorse timer failed")
      }
    case _ ⇒ log.error("Remorse timer is in trouble, it can't communicate properly :(")
  }
}
