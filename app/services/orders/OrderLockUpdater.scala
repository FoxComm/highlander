package services.orders

import java.time.{Duration, Instant}

import models.{Order, OrderLockEvent, OrderLockEvents, Orders, StoreAdmin}
import responses.FullOrder
import services.Result
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object OrderLockUpdater {

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustNotBeLocked
    _     ← * <~ Orders.update(order, order.copy(isLocked = true))
    _     ← * <~ OrderLockEvents.create(OrderLockEvent(orderId = order.id, lockedBy = admin.id))
    resp  ← * <~ FullOrder.refreshAndFullOrder(order).toXor
  } yield resp).runT()

  def unlock(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    def increaseRemorse(order: Order)(duration: Duration) = order.remorsePeriodEnd.map(_.plus(duration))
    (for {
      order    ← * <~ Orders.mustFindByRefNum(refNum)
      _        ← * <~ order.mustBeLocked
      lastLock ← * <~ OrderLockEvents.latestLockByOrder(order.id).one.toXor
      remorsePlus = increaseRemorse(order) _
      newEnd   ← * <~ lastLock.fold(remorsePlus(Duration.ofMinutes(15)))(lock ⇒ remorsePlus(Duration.between(lock.lockedAt, Instant.now)))
      _        ← * <~ Orders.update(order, order.copy(isLocked = false, remorsePeriodEnd = newEnd))
      response ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    } yield response).runT()
  }
}
