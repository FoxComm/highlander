package models.cord

import java.time.Instant

import models.StoreAdmins
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CartLockEvent(id: Int = 0,
                         cartRef: String,
                         lockedAt: Instant = Instant.now,
                         lockedBy: Int = 0)
    extends FoxModel[CartLockEvent]

class CartLockEvents(tag: Tag) extends FoxTable[CartLockEvent](tag, "cart_lock_events") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cartRef  = column[String]("cart_ref")
  def lockedAt = column[Instant]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def * =
    (id, cartRef, lockedAt, lockedBy) <> ((CartLockEvent.apply _).tupled, CartLockEvent.unapply)

  def storeAdmin = foreignKey(StoreAdmins.tableName, lockedBy, StoreAdmins)(_.id)
}

object CartLockEvents
    extends FoxTableQuery[CartLockEvent, CartLockEvents](new CartLockEvents(_))
    with ReturningId[CartLockEvent, CartLockEvents] {

  val returningLens: Lens[CartLockEvent, Int] = lens[CartLockEvent].id

  import scope._

  def findByCartRef(cartRef: String): QuerySeq =
    filter(_.cartRef === cartRef)

  def latestLockByCartRef(cartRef: String): QuerySeq =
    findByCartRef(cartRef).mostRecentLock

  object scope {
    implicit class OrderLockEventsQuerySeqConversions(q: QuerySeq) {
      val mostRecentLock: QuerySeq = {
        q.sortBy(_.lockedAt).take(1)
      }
    }
  }
}
