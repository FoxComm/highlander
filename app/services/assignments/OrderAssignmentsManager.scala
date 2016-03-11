package services.assignments

import models.Assignment
import models.order._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object OrderAssignmentsManager extends AssignmentsManager[String, Order] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Assignee
  def referenceType(): Assignment.ReferenceType = Assignment.Order
  def notifyDimension(): String = models.activity.Dimension.order

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Order] =
    Orders.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Order]] =
    Orders.filter(_.referenceNumber.inSetBind(refNums)).result.toXor
}
