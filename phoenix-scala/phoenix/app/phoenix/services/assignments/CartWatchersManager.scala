package phoenix.services.assignments

import phoenix.models.activity.Dimension
import phoenix.models.cord._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.cord.CartResponse
import phoenix.responses.cord.CartResponse.buildEmpty
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import core.db._

object CartWatchersManager extends AssignmentsManager[String, Cart] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Cart
  val notifyDimension = Dimension.cart
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Cart): CartResponse = buildEmpty(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Cart] =
    Carts.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Cart]] =
    Carts.filter(_.referenceNumber.inSetBind(refNums)).result.dbresult
}
