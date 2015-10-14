package services

import java.time.{Duration, Instant}
import utils.time._
import scala.concurrent.ExecutionContext
import models.Order.RemorseHold
import models.OrderLockEvents.scope._
import models._
import responses.{StoreAdminResponse, FullOrder, FullOrderWithWarnings}
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import utils.Slick._
import utils.Slick.implicits._

object LockAwareOrderUpdater {

  private val updatedOrder = Orders.map(identity)

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdate { order ⇒
      val lock = finder.map(_.locked).updateReturning(updatedOrder, true).head
      val blame = OrderLockEvents += OrderLockEvent(orderId = order.id, lockedBy = admin.id)

      DbResult.fromDbio(blame >> lock.flatMap(FullOrder.fromOrder))
    }
  }

  def increaseRemorsePeriod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdate { order ⇒
      order.status match {
        case RemorseHold ⇒
          DbResult.fromDbio(finder
            .map(_.remorsePeriodEnd)
            .updateReturning(updatedOrder, order.remorsePeriodEnd.map(_.plusMinutes(15))).head
            .flatMap(FullOrder.fromOrder))

        case _ ⇒ DbResult.failure(GeneralFailure("Order is not in RemorseHold status"))
      }
    }
  }

  private def doUnlock(orderId: Int, remorseEnd: Option[Instant])(implicit ec: ExecutionContext, db: Database) = {
    Orders.findById(orderId).extract
      .map { o ⇒ (o.locked, o.remorsePeriodEnd) }
      .updateReturning(updatedOrder, (false, remorseEnd)).head
      .flatMap { o ⇒ DbResult.fromDbio(FullOrder.fromOrder(o)) }
  }

  def unlock(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    Orders.findByRefNum(refNum).selectOneForUpdateIgnoringLock { order ⇒
      if (order.locked) {
        OrderLockEvents.findByOrder(order).mostRecentLock.one.flatMap {
          case Some(lockEvent) ⇒
            val lockedPeriod = Duration.between(lockEvent.lockedAt, Instant.now)
            val newEnd = order.remorsePeriodEnd.map(_.plus(lockedPeriod))
            doUnlock(order.id, newEnd)
          case None ⇒
            doUnlock(order.id, order.remorsePeriodEnd.map(_.plusMinutes(15)))
        }
      } else DbResult.failure(GeneralFailure("Order is not locked"))
    }
  }

  def assign(refNum: String, requestedAssigneeIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext): Result[FullOrderWithWarnings] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdateIgnoringLock { order ⇒
      DbResult.fromDbio(for {
        existingAdminIds ← StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result

        alreadyAssigned ← OrderAssignments.assigneesFor(order)
        alreadyAssignedIds = alreadyAssigned.map(_.id)

        newAssignments = existingAdminIds.diff(alreadyAssignedIds)
          .map(adminId ⇒ OrderAssignment(orderId = order.id, assigneeId = adminId))

        inserts = OrderAssignments ++= newAssignments
        newOrder ← inserts >> finder.result.head

        fullOrder ← FullOrder.fromOrder(newOrder)
        warnings = requestedAssigneeIds.diff(existingAdminIds).map(NotFoundFailure(StoreAdmin, _))
      } yield FullOrderWithWarnings(fullOrder, warnings))
    }
  }
}
