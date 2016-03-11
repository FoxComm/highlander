package services.assignments

import models.Assignment
import models.customer._
import utils.Slick._
import utils.aliases._

object CustomerAssignmentsManager extends AssignmentsManager[Int, Customer] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Assignee
  def referenceType(): Assignment.ReferenceType = Assignment.Customer
  def notifyDimension(): String = models.activity.Dimension.customer

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Customer] =
    Customers.mustFindById404(id)
}
