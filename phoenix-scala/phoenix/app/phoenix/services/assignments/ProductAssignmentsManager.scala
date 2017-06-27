package phoenix.services.assignments

import phoenix.models.activity.Dimension
import phoenix.models.product._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.ProductResponses.ProductHeadResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import core.db._

object ProductAssignmentsManager extends AssignmentsManager[Int, Product] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Product
  val notifyDimension = Dimension.product
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Product): ProductHeadResponse = ProductHeadResponse.build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Product] =
    Products.mustFindById404(id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Product]] =
    Products.filter(_.id.inSetBind(ids)).result.dbresult
}
