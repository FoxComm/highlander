package phoenix.services.assignments

import phoenix.models.cord._
import phoenix.models.activity.Dimension
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.cord.AmazonOrderResponse
import phoenix.responses.cord.AmazonOrderResponse.build
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import core.db._

object AmazonOrderWatchersManager extends AssignmentsManager[String, AmazonOrder] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.AmazonOrder
  val notifyDimension = Dimension.amazonOrder
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: AmazonOrder): AmazonOrderResponse = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[AmazonOrder] =
    AmazonOrders.mustFindOneOr(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[AmazonOrder]] =
    AmazonOrders.filter(_.amazonOrderId.inSetBind(refNums)).result.dbresult
}
