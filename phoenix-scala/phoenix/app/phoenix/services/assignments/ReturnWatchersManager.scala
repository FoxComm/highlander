package phoenix.services.assignments

import core.db._
import phoenix.models.activity.Dimension
import phoenix.models.returns._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.ReturnResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object ReturnWatchersManager extends AssignmentsManager[String, Return] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Return
  val notifyDimension = Dimension.rma
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Return): ReturnResponse = ReturnResponse.build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Return] =
    Returns.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Return]] =
    Returns.filter(_.referenceNumber.inSetBind(refNums)).result.dbresult
}
