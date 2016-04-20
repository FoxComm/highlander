package services.orders

import java.time.{Duration, Instant}

import models.order._
import models.StoreAdmin
import responses.order.FullOrder
import services.Result
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object OrderLockUpdater {

  def lock(refNum: String, admin: StoreAdmin)(implicit ec: EC, db: DB): Result[FullOrder.Root] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustNotBeLocked
    _     ← * <~ Orders.update(order, order.copy(isLocked = true))
    _     ← * <~ OrderLockEvents.create(OrderLockEvent(orderId = order.id, lockedBy = admin.id))
    resp  ← * <~ FullOrder.refreshAndFullOrder(order).toXor
  } yield resp).runTxn()

  def unlock(refNum: String)(implicit ec: EC, db: DB): Result[FullOrder.Root] = {
    def increaseRemorse(order: Order)(duration: Duration) = order.remorsePeriodEnd.map(_.plus(duration))
    (for {
      order    ← * <~ Orders.mustFindByRefNum(refNum)
      _        ← * <~ order.mustBeLocked
      lastLock ← * <~ OrderLockEvents.latestLockByOrder(order.id).one.toXor
      remorsePlus = increaseRemorse(order) _
      newEnd   ← * <~ lastLock.fold(remorsePlus(Duration.ofMinutes(15)))(lock ⇒ remorsePlus(Duration.between(lock.lockedAt, Instant.now)))
      _        ← * <~ Orders.update(order, order.copy(isLocked = false, remorsePeriodEnd = newEnd))
      response ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    } yield response).runTxn()
  }
}
