package models

import com.github.tototoshi.slick.PostgresJodaSupport._
import monocle.macros.GenLens
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class OrderLockEvent(id: Int = 0, orderId: Int = 0, lockedAt: DateTime = DateTime.now, lockedBy: Int = 0)
  extends ModelWithIdParameter

class OrderLockEvents(tag: Tag) extends GenericTable.TableWithId[OrderLockEvent](tag, "order_lock_events") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def lockedAt = column[DateTime]("locked_at")
  def lockedBy = column[Int]("locked_by")
  def * = (id, orderId, lockedAt, lockedBy) <>((OrderLockEvent.apply _).tupled, OrderLockEvent.unapply)
}

object OrderLockEvents extends TableQueryWithId[OrderLockEvent, OrderLockEvents](
  idLens = GenLens[OrderLockEvent](_.id)
)(new OrderLockEvents(_)) {

  def findByOrder(order: Order): Query[OrderLockEvents, OrderLockEvent, Seq] =
    filter(_.orderId === order.id)

  object scope {
    implicit class OrderLockEventsQuerySeqConversions(q: QuerySeq) {
      val mostRecentLock: QuerySeq = {
        q.sortBy(_.lockedAt).take(1)
      }
    }
  }
}
