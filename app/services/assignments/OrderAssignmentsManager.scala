package services.assignments

import models.Assignment
import models.order._
import utils.Slick._
import utils.aliases._

object OrderAssignmentsManager extends AssignmentsManager[String, Order] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Assignee
  def referenceType(): Assignment.ReferenceType = Assignment.Order

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Order] =
    Orders.mustFindByRefNum(refNum)
}
