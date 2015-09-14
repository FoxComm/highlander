package services

import java.time.Instant

import scala.concurrent.ExecutionContext


import models.Order.RemorseHold
import models._
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import utils.Slick._

object LockAwareOrderUpdater {

  private val updatedOrder = Orders.map(identity)

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.findOneAndRun { order ⇒
      val lock = finder.map(_.locked).updateReturning(updatedOrder, true).head
      val blame = OrderLockEvents += OrderLockEvent(orderId = order.id, lockedBy = admin.id)

      DbResult.dbio(blame >> lock.flatMap(o ⇒ DbResult.liftFuture(FullOrder.fromOrder(o))))
    }
  }

  // Should never be None as this response is sent only when increasing remorse period
  final class NewRemorsePeriodEnd(val remorsePeriodEnd: Option[Instant])

  def increaseRemorsePeriod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[NewRemorsePeriodEnd] = {
    val finder = Orders.findByRefNum(refNum)

    finder.findOneAndRun { order ⇒
      order.status match {
        case RemorseHold ⇒
          DbResult.dbio(finder
            .map(_.remorsePeriodEnd)
            .updateReturning(updatedOrder, order.remorsePeriodEnd.map(_.plusSeconds(15 * 60))).head
            .map(order ⇒ new NewRemorsePeriodEnd(order.remorsePeriodEnd)))

        case _ ⇒ DbResult.failure(GeneralFailure("Order is not in RemorseHold status"))
      }
    }
  }
}
