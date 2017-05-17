package services.assignments

import failures.NotFoundFailure404
import models.objects.{ObjectForm, ObjectForms}
import models.promotion.Promotion
import models.{Assignment, NotificationSubscription}
import responses.PromotionResponses.PromotionFormResponse._
import slick.jdbc.PostgresProfile.api._
import utils.db._
import utils.aliases._

object PromotionWatchersManager extends AssignmentsManager[Int, ObjectForm] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Promotion
  val notifyDimension = models.activity.Dimension.promotion
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: ObjectForm): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[ObjectForm] =
    ObjectForms
      .filter(_.kind === ObjectForm.promotion)
      .mustFindOneOr(NotFoundFailure404(Promotion, id))

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[ObjectForm]] =
    ObjectForms.filter(_.kind === ObjectForm.promotion).filter(_.id.inSetBind(ids)).result.dbresult
}
