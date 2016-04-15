package services.assignments

import failures.NotFoundFailure404
import models.objects.{ObjectForm, ObjectForms}
import models.promotion.Promotion
import models.{Assignment, NotificationSubscription}
import responses.PromotionResponses.PromotionFormResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object PromotionAssignmentsManager extends AssignmentsManager[Int, ObjectForm] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Promotion
  val notifyDimension = models.activity.Dimension.promotion
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: ObjectForm): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[ObjectForm] =
    ObjectForms.filter(_.kind === ObjectForm.promotion).one.mustFindOr(NotFoundFailure404(Promotion, id))

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[ObjectForm]] =
    ObjectForms.filter(_.kind === ObjectForm.promotion).filter(_.id.inSetBind(ids)).result.toXor
}
