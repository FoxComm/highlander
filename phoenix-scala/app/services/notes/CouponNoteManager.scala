package services.notes

import failures.CouponFailures.CouponNotFoundForContext
import models.Note
import models.coupon.Coupons
import models.objects.{IlluminatedObject, ObjectForms, ObjectShadows}
import utils.aliases._
import utils.db._

object CouponNoteManager extends NoteManager[Int, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Coupon

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      coupon ← * <~ Coupons
                .filterByContextAndFormId(defaultContextId, id)
                .mustFindOneOr(CouponNotFoundForContext(id, "<default context>"))
      form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)


}
