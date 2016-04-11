package services.assignments

import models.{Assignment, NotificationSubscription}
import models.rma._
import responses.AllRmas._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils._
import utils.aliases._

object RmaAssignmentsManager extends AssignmentsManager[String, Rma] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Rma
  val notifyDimension = models.activity.Dimension.rma
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Rma): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Rma] =
    Rmas.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Rma]] =
    Rmas.filter(_.referenceNumber.inSetBind(refNums)).result.toXor
}
