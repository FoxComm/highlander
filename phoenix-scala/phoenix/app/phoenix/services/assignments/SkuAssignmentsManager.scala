package phoenix.services.assignments

import core.db._
import phoenix.models.activity.Dimension
import phoenix.models.inventory.{Sku, Skus}
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.SkuResponses.SkuHeadResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object SkuAssignmentsManager extends AssignmentsManager[String, Sku] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Sku
  val notifyDimension = Dimension.sku
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Sku): SkuHeadResponse = SkuHeadResponse.build(model)

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Sku] =
    Skus.mustFindByCode(code)

  def fetchSequence(codes: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Sku]] =
    Skus.filter(_.code.inSetBind(codes)).result.dbresult
}
