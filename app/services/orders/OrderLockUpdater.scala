package services.orders

import java.time.{Duration, Instant}

import scala.concurrent.ExecutionContext

import models._
import responses.FullOrder
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import utils.time._

object OrderLockUpdater {

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdate { order ⇒
      val lock = finder.map(_.locked).updateReturningHead(Orders.map(identity), true)
      val blame = OrderLockEvents += OrderLockEvent(orderId = order.id, lockedBy = admin.id) // FIXME after #522

      lock.flatMap(xor ⇒ xorMapDbio(xor)(order ⇒ blame >> FullOrder.fromOrder(order)))
    }
  }

  private def doUnlock(orderId: Int, remorseEnd: Option[Instant])(implicit ec: ExecutionContext, db: Database) = {
    Orders.findById(orderId).extract
      .map { o ⇒ (o.locked, o.remorsePeriodEnd) }
      .updateReturningHead(Orders.map(identity), (false, remorseEnd))
      .flatMap { xor ⇒ xorMapDbio(xor)(FullOrder.fromOrder) }
  }

  def unlock(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    Orders.findByRefNum(refNum).selectOneForUpdate({ order ⇒
      if (order.locked) {
        OrderLockEvents.latestLockByOrder(order.id).one.flatMap {
          case Some(lockEvent) ⇒
            val lockedPeriod = Duration.between(lockEvent.lockedAt, Instant.now)
            val newEnd = order.remorsePeriodEnd.map(_.plus(lockedPeriod))
            doUnlock(order.id, newEnd)
          case None ⇒
            doUnlock(order.id, order.remorsePeriodEnd.map(_.plusMinutes(15)))
        }
      } else DbResult.failure(GeneralFailure("Order is not locked"))
    }, checks = Set.empty)
  }
}
