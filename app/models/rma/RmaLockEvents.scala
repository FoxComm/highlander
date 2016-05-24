package models.rma

import java.time.Instant

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class RmaLockEvent(
    id: Int = 0, rmaId: Int = 0, lockedAt: Instant = Instant.now, lockedBy: Int = 0)
    extends FoxModel[RmaLockEvent]

class RmaLockEvents(tag: Tag) extends FoxTable[RmaLockEvent](tag, "rma_lock_events") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId    = column[Int]("rma_id")
  def lockedAt = column[Instant]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def *        = (id, rmaId, lockedAt, lockedBy) <> ((RmaLockEvent.apply _).tupled, RmaLockEvent.unapply)
}

object RmaLockEvents
    extends FoxTableQuery[RmaLockEvent, RmaLockEvents](new RmaLockEvents(_))
    with ReturningId[RmaLockEvent, RmaLockEvents] {

  val returningLens: Lens[RmaLockEvent, Int] = lens[RmaLockEvent].id

  import scope._

  def findByRma(rmaId: Rma#Id): QuerySeq =
    filter(_.rmaId === rmaId)

  def latestLockByRma(rmaId: Rma#Id): QuerySeq =
    findByRma(rmaId).mostRecentLock

  object scope {
    implicit class RmaLockEventsQuerySeqConversions(q: QuerySeq) {
      val mostRecentLock: QuerySeq = {
        q.sortBy(_.lockedAt).take(1)
      }
    }
  }
}
