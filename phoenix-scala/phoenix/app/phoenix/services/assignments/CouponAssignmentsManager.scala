package phoenix.services.assignments

import core.db._
import core.failures.NotFoundFailure404
import objectframework.models.{ObjectForm, ObjectForms}
import phoenix.models.activity.Dimension
import phoenix.models.coupon.Coupon
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.CouponResponses.CouponFormResponse._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object CouponAssignmentsManager extends AssignmentsManager[Int, ObjectForm] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Coupon
  val notifyDimension = Dimension.coupon
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: ObjectForm): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[ObjectForm] =
    ObjectForms
      .filter { f ⇒
        f.kind === ObjectForm.coupon && f.id === id
      }
      .mustFindOneOr(NotFoundFailure404(Coupon, id))

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[ObjectForm]] =
    ObjectForms
      .filter { f ⇒
        f.kind === ObjectForm.coupon && f.id.inSet(ids)
      }
      .filter(_.id.inSetBind(ids))
      .result
      .dbresult
}
