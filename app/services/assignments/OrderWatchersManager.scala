package services.assignments

import models.{Assignment, NotificationSubscription}
import models.order._
import responses.order.AllOrders._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object OrderWatchersManager extends AssignmentsManager[String, Order] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Order
  val notifyDimension = models.activity.Dimension.order
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Order): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Order] =
    Orders.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Order]] =
    Orders.filter(_.referenceNumber.inSetBind(refNums)).result.toXor
}
