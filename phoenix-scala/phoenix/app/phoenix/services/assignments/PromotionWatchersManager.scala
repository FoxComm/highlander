package phoenix.services.assignments

import core.db._
import core.failures.NotFoundFailure404
import objectframework.models.{ObjectForm, ObjectForms}
import phoenix.models.activity.Dimension
import phoenix.models.promotion.Promotion
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.PromotionResponses.PromotionFormResponse._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object PromotionWatchersManager extends AssignmentsManager[Int, ObjectForm] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Promotion
  val notifyDimension = Dimension.promotion
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: ObjectForm): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[ObjectForm] =
    ObjectForms
      .filter(_.kind === ObjectForm.promotion)
      .mustFindOneOr(NotFoundFailure404(Promotion, id))

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[ObjectForm]] =
    ObjectForms.filter(_.kind === ObjectForm.promotion).filter(_.id.inSetBind(ids)).result.dbresult
}
