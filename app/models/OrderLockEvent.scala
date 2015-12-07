package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class OrderLockEvent(id: Int = 0, orderId: Int = 0, lockedAt: Instant = Instant.now, lockedBy: Int = 0)
  extends ModelWithIdParameter[OrderLockEvent]

class OrderLockEvents(tag: Tag) extends GenericTable.TableWithId[OrderLockEvent](tag, "order_lock_events") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def lockedAt = column[Instant]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def * = (id, orderId, lockedAt, lockedBy) <>((OrderLockEvent.apply _).tupled, OrderLockEvent.unapply)

  def storeAdmin = foreignKey(StoreAdmins.tableName, lockedBy, StoreAdmins)(_.id)
}

object OrderLockEvents extends TableQueryWithId[OrderLockEvent, OrderLockEvents](
  idLens = GenLens[OrderLockEvent](_.id)
)(new OrderLockEvents(_)) {

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
