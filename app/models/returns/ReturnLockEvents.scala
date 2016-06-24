package models.returns

import java.time.Instant

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class ReturnLockEvent(id: Int = 0,
                           returnId: Int = 0,
                           lockedAt: Instant = Instant.now,
                           lockedBy: Int = 0)
    extends FoxModel[ReturnLockEvent]

class ReturnLockEvents(tag: Tag) extends FoxTable[ReturnLockEvent](tag, "return_lock_events") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId = column[Int]("return_id")
  def lockedAt = column[Instant]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def * =
    (id, returnId, lockedAt, lockedBy) <> ((ReturnLockEvent.apply _).tupled, ReturnLockEvent.unapply)
}

object ReturnLockEvents
    extends FoxTableQuery[ReturnLockEvent, ReturnLockEvents](new ReturnLockEvents(_))
    with ReturningId[ReturnLockEvent, ReturnLockEvents] {

  val returningLens: Lens[ReturnLockEvent, Int] = lens[ReturnLockEvent].id

  import scope._

  def findByRma(returnId: Return#Id): QuerySeq =
    filter(_.returnId === returnId)

  def latestLockByRma(returnId: Return#Id): QuerySeq =
    findByRma(returnId).mostRecentLock

  object scope {
    implicit class RmaLockEventsQuerySeqConversions(q: QuerySeq) {
      val mostRecentLock: QuerySeq = {
        q.sortBy(_.lockedAt).take(1)
      }
    }
  }
}
