package services.assignments

import models.Assignment
import models.rma._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object RmaWatchersManager extends AssignmentsManager[String, Rma] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Watcher
  def referenceType(): Assignment.ReferenceType = Assignment.Rma
  def notifyDimension(): String = models.activity.Dimension.rma

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Rma] =
    Rmas.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Rma]] =
    Rmas.filter(_.referenceNumber.inSetBind(refNums)).result.toXor
}
