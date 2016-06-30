package models.order

import java.time.Instant

import models.StoreAdmins
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class OrderLockEvent(id: Int = 0,
                          orderRef: String,
                          lockedAt: Instant = Instant.now,
                          lockedBy: Int = 0)
    extends FoxModel[OrderLockEvent]

class OrderLockEvents(tag: Tag) extends FoxTable[OrderLockEvent](tag, "order_lock_events") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderRef = column[String]("order_ref")
  def lockedAt = column[Instant]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def * =
    (id, orderRef, lockedAt, lockedBy) <> ((OrderLockEvent.apply _).tupled, OrderLockEvent.unapply)

  def storeAdmin = foreignKey(StoreAdmins.tableName, lockedBy, StoreAdmins)(_.id)
}

object OrderLockEvents
    extends FoxTableQuery[OrderLockEvent, OrderLockEvents](new OrderLockEvents(_))
    with ReturningId[OrderLockEvent, OrderLockEvents] {

  val returningLens: Lens[OrderLockEvent, Int] = lens[OrderLockEvent].id

  import scope._

  def findByOrder(orderRef: String): QuerySeq =
    filter(_.orderRef === orderRef)

  def latestLockByOrder(orderRef: String): QuerySeq =
    findByOrder(orderRef).mostRecentLock

  object scope {
    implicit class OrderLockEventsQuerySeqConversions(q: QuerySeq) {
      val mostRecentLock: QuerySeq = {
        q.sortBy(_.lockedAt).take(1)
      }
    }
  }
}
