package services.assignments

import failures.NotFoundFailure404
import models.objects.{ObjectForm, ObjectForms}
import models.coupon.Coupon
import models.{Assignment, NotificationSubscription}
import responses.CouponResponses.CouponFormResponse._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CouponAssignmentsManager extends AssignmentsManager[Int, ObjectForm] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Coupon
  val notifyDimension = models.activity.Dimension.coupon
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: ObjectForm): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[ObjectForm] =
    ObjectForms.filter(_.kind === ObjectForm.coupon).mustFindOneOr(NotFoundFailure404(Coupon, id))

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[ObjectForm]] =
    ObjectForms.filter(_.kind === ObjectForm.coupon).filter(_.id.inSetBind(ids)).result.toXor
}
