package services.assignments

import models.{Assignment, NotificationSubscription}
import models.order._
import responses.order.AllOrders._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object OrderAssignmentsManager extends AssignmentsManager[String, Order] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Order
  val notifyDimension = models.activity.Dimension.order
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Order): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Order] =
    Orders.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Order]] =
    Orders.filter(_.referenceNumber.inSetBind(refNums)).result.toXor
}
