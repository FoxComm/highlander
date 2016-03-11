package services.assignments

import models.Assignment
import models.customer._
import utils.Slick._
import utils.aliases._

object CustomerWatchersManager extends AssignmentsManager[Int, Customer] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Watcher
  def referenceType(): Assignment.ReferenceType = Assignment.Customer

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Customer] =
    Customers.mustFindById404(id)
}
