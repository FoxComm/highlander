package phoenix.services.notes

import core.db._
import objectframework.models.{IlluminatedObject, ObjectForms, ObjectShadows}
import phoenix.failures.CouponFailures._
import phoenix.models.Note
import phoenix.models.coupon.Coupons
import phoenix.utils.aliases._

object CouponNoteManager extends NoteManager[Int, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Coupon

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      coupon ← * <~ Coupons
                .filterByContextAndFormId(defaultContextId, id)
                .mustFindOneOr(CouponNotFoundForDefaultContext(id))
      form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)

}
