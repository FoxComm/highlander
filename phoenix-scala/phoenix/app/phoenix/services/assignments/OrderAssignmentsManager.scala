package phoenix.services.assignments

import phoenix.models.activity.Dimension
import phoenix.models.cord._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.cord.AllOrders._
import slick.jdbc.PostgresProfile.api._
import phoenix.utils.aliases._
import core.db._

object OrderAssignmentsManager extends AssignmentsManager[String, Order] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Order
  val notifyDimension = Dimension.order
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Order): Root = build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Order] =
    Orders.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Order]] =
    Orders.filter(_.referenceNumber.inSetBind(refNums)).result.dbresult
}
