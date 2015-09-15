package services

import java.time.{Duration, Instant}
import utils.time._
import scala.concurrent.ExecutionContext

import models.Order.RemorseHold
import models.OrderLockEvents.scope._
import models._
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import utils.Slick._
import utils.Slick.implicits._

object LockAwareOrderUpdater {

  private val updatedOrder = Orders.map(identity)

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.findOneAndRun { order ⇒
      val lock = finder.map(_.locked).updateReturning(updatedOrder, true).head
      val blame = OrderLockEvents += OrderLockEvent(orderId = order.id, lockedBy = admin.id)

      DbResult.fromDbio(blame >> lock.flatMap(o ⇒ liftFuture(FullOrder.fromOrder(o))))
    }
  }

  // Should never be None as this response is sent only when increasing remorse period
  final class NewRemorsePeriodEnd(val remorsePeriodEnd: Option[Instant])

  def increaseRemorsePeriod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[NewRemorsePeriodEnd] = {
    val finder = Orders.findByRefNum(refNum)

    finder.findOneAndRun { order ⇒
      order.status match {
        case RemorseHold ⇒
          DbResult.fromDbio(finder
            .map(_.remorsePeriodEnd)
            .updateReturning(updatedOrder, order.remorsePeriodEnd.map(_.plusMinutes(15))).head
            .map(order ⇒ new NewRemorsePeriodEnd(order.remorsePeriodEnd)))

        case _ ⇒ DbResult.failure(GeneralFailure("Order is not in RemorseHold status"))
      }
    }
  }

  private def doUnlock(orderId: Int, remorseEnd: Option[Instant])(implicit ec: ExecutionContext, db: Database) = {
    Orders._findById(orderId).extract
      .map { o ⇒ (o.locked, o.remorsePeriodEnd) }
      .updateReturning(updatedOrder, (false, remorseEnd)).head
      .flatMap { o ⇒ DbResult.fromFuture(FullOrder.fromOrder(o)) }
  }

  def unlock(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    Orders.findByRefNum(refNum).findOneAndRunIgnoringLock { order ⇒
      if (order.locked) {
        OrderLockEvents.findByOrder(order).mostRecentLock.one.flatMap {
          case Some(lockEvent) ⇒
            val lockedPeriod = Duration.between(lockEvent.lockedAt, Instant.now)
            val newEnd = order.remorsePeriodEnd.map(_.plus(lockedPeriod))
            doUnlock(order.id, newEnd)
          case None ⇒
            doUnlock(order.id, order.remorsePeriodEnd.map(_.plusMinutes(15)))
        }
      } else DbResult.failure(GeneralFailure("Order is not locked"))
    }
  }
}
