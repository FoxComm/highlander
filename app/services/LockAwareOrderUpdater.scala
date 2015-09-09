package services

import scala.concurrent.ExecutionContext

import models._
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.UpdateReturning._

object LockAwareOrderUpdater {

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.findOneAndRun { order ⇒
      val lock = finder.map(_.locked).updateReturning(Orders.map(identity), true).head
      val blame = OrderLockEvents += OrderLockEvent(orderId = order.id, lockedBy = admin.id)

      DbResult.dbio(blame >> lock.flatMap(o ⇒ DbResult.liftFuture(FullOrder.fromOrder(o))))
    }
  }
}
