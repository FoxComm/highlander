package services.assignments

import models.{Assignment, NotificationSubscription}
import models.product._
import responses.ProductResponses.ProductHeadResponse._
import slick.jdbc.PostgresProfile.api._
import utils.db._
import utils.aliases._

object ProductAssignmentsManager extends AssignmentsManager[Int, Product] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Product
  val notifyDimension = models.activity.Dimension.product
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Product): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Product] =
    Products.mustFindById404(id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Product]] =
    Products.filter(_.id.inSetBind(ids)).result.dbresult
}
