package services.assignments

import models.cord._
import models.{Assignment, NotificationSubscription}
import responses.cord.AllOrders._
import slick.jdbc.PostgresProfile.api._
import utils.aliases._
import utils.db._

object OrderAssignmentsManager extends AssignmentsManager[String, Order] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Order
  val notifyDimension = models.activity.Dimension.order
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Order): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Order] =
    Orders.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Order]] =
    Orders.filter(_.referenceNumber.inSetBind(refNums)).result.dbresult
}
