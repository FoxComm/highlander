package services.assignments

import models.Assignment
import models.order._
import utils.Slick._
import utils.aliases._

object OrderWatchersManager extends AssignmentsManager[String, Order] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Watcher
  def referenceType(): Assignment.ReferenceType = Assignment.Order
  def notifyDimension(): String = models.activity.Dimension.order

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Order] =
    Orders.mustFindByRefNum(refNum)
}
