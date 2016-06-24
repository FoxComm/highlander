package models.order

import java.time.Instant

import models.StoreAdmins
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class OrderLockEvent(id: Int = 0,
                          orderId: Int = 0,
                          lockedAt: Instant = Instant.now,
                          lockedBy: Int = 0)
    extends FoxModel[OrderLockEvent]

class OrderLockEvents(tag: Tag) extends FoxTable[OrderLockEvent](tag, "order_lock_events") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId  = column[Int]("order_id")
  def lockedAt = column[Instant]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def * =
    (id, orderId, lockedAt, lockedBy) <> ((OrderLockEvent.apply _).tupled, OrderLockEvent.unapply)

  def storeAdmin = foreignKey(StoreAdmins.tableName, lockedBy, StoreAdmins)(_.id)
}

object OrderLockEvents
    extends FoxTableQuery[OrderLockEvent, OrderLockEvents](new OrderLockEvents(_))
    with ReturningId[OrderLockEvent, OrderLockEvents] {

  val returningLens: Lens[OrderLockEvent, Int] = lens[OrderLockEvent].id

  import scope._

  def findByOrder(orderId: Int): QuerySeq =
    filter(_.orderId === orderId)

  def latestLockByOrder(orderId: Int): QuerySeq =
    findByOrder(orderId).mostRecentLock

  object scope {
    implicit class OrderLockEventsQuerySeqConversions(q: QuerySeq) {
      val mostRecentLock: QuerySeq = {
        q.sortBy(_.lockedAt).take(1)
      }
    }
  }
}
