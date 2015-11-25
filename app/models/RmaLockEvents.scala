package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaLockEvent(id: Int = 0, rmaId: Int = 0, lockedAt: Instant = Instant.now, lockedBy: Int = 0)
  extends ModelWithIdParameter[RmaLockEvent]

class RmaLockEvents(tag: Tag) extends GenericTable.TableWithId[RmaLockEvent](tag, "rma_lock_events") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def lockedAt = column[Instant]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def * = (id, rmaId, lockedAt, lockedBy) <>((RmaLockEvent.apply _).tupled, RmaLockEvent.unapply)
}

object RmaLockEvents extends TableQueryWithId[RmaLockEvent, RmaLockEvents](
  idLens = GenLens[RmaLockEvent](_.id)
)(new RmaLockEvents(_)) {

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
