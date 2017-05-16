package services.assignments

import models.cord._
import models.{Assignment, NotificationSubscription}
import responses.cord.CartResponse
import responses.cord.CartResponse.buildEmpty
import slick.jdbc.PostgresProfile.api._
import utils.aliases._
import utils.db._

object CartWatchersManager extends AssignmentsManager[String, Cart] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Cart
  val notifyDimension = models.activity.Dimension.cart
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Cart): CartResponse = buildEmpty(model)

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Cart] =
    Carts.mustFindByRefNum(refNum)

  def fetchSequence(refNums: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Cart]] =
    Carts.filter(_.referenceNumber.inSetBind(refNums)).result.dbresult
}
