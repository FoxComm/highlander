package phoenix.services.assignments

import core.db._
import core.failures.NotFoundFailure404
import objectframework.models.{ObjectForm, ObjectForms}
import phoenix.models.activity.Dimension
import phoenix.models.coupon.Coupon
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.CouponResponses.CouponFormResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object CouponWatchersManager extends AssignmentsManager[Int, ObjectForm] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Coupon
  val notifyDimension = Dimension.coupon
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: ObjectForm): CouponFormResponse = CouponFormResponse.build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[ObjectForm] =
    ObjectForms.filter(_.kind === ObjectForm.coupon).mustFindOneOr(NotFoundFailure404(Coupon, id))

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[ObjectForm]] =
    ObjectForms.filter(_.kind === ObjectForm.coupon).filter(_.id.inSetBind(ids)).result.dbresult
}
