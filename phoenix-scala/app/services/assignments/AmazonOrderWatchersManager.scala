package services.assignments

import models.cord._
import models.{Assignment, NotificationSubscription}
import responses.cord.AmazonOrderResponse._
import slick.jdbc.PostgresProfile.api._
import utils.aliases._
import utils.db._

object AmazonOrderWatchersManager extends AssignmentsManager[String, AmazonOrder] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.AmazonOrder
  val notifyDimension = models.activity.Dimension.amazonOrder
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: AmazonOrder): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[AmazonOrder] =
    AmazonOrders.mustFindByAmazonOrderId(refNum)

  def fetchSequence(
      refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[AmazonOrder]] =
    AmazonOrders.filter(_.amazonOrderId.inSetBind(refNums)).result.dbresult
}
