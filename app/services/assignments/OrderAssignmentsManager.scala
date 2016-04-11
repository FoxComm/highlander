package services.assignments

import models.Assignment
import models.order._
import responses.order.AllOrders._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object OrderAssignmentsManager extends AssignmentsManager[String, Order] {

  val assignmentType: Assignment.AssignmentType = Assignment.Assignee
  val referenceType: Assignment.ReferenceType = Assignment.Order
  val notifyDimension: String = models.activity.Dimension.order

  def buildResponse(model: Order): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Order] =
    Orders.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Order]] =
    Orders.filter(_.referenceNumber.inSetBind(refNums)).result.toXor
}
