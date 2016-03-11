package services.assignments

import models.Assignment
import models.rma._
import utils.Slick._
import utils.aliases._

object RmaAssignmentsManager extends AssignmentsManager[String, Rma] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Assignee
  def referenceType(): Assignment.ReferenceType = Assignment.Rma

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Rma] =
    Rmas.mustFindByRefNum(refNum)
}
