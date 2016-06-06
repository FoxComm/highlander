package services.assignments

import models.{Assignment, NotificationSubscription}
import models.returns._
import responses.AllReturns._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object ReturnWatchersManager extends AssignmentsManager[String, Return] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Return
  val notifyDimension = models.activity.Dimension.rma
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Return): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Return] =
    Returns.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Return]] =
    Returns.filter(_.referenceNumber.inSetBind(refNums)).result.toXor
}
