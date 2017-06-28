package phoenix.services.assignments

import core.db._
import phoenix.models.activity.Dimension
import phoenix.models.cord._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.cord.AllOrders
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object OrderWatchersManager extends AssignmentsManager[String, Order] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Order
  val notifyDimension = Dimension.order
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Order): AllOrders = AllOrders.build(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Order] =
    Orders.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Order]] =
    Orders.filter(_.referenceNumber.inSetBind(refNums)).result.dbresult
}
