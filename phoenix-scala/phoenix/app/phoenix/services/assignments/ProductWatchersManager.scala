package phoenix.services.assignments

import phoenix.models.activity.Dimension
import phoenix.models.product._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.ProductResponses.ProductHeadResponse._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import utils.db._

object ProductWatchersManager extends AssignmentsManager[Int, Product] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Product
  val notifyDimension = Dimension.product
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Product): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Product] =
    Products.mustFindById404(id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Product]] =
    Products.filter(_.id.inSetBind(ids)).result.dbresult
}
