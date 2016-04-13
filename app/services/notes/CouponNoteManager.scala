package services.notes

import models.Note
import models.coupon.{Coupon, Coupons}
import utils.Slick._
import utils.aliases._

object CouponNoteManager extends NoteManager[Int, Coupon] {

  def noteType(): Note.ReferenceType = Note.Coupon

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Coupon] =
    Coupons.mustFindById404(id)
}
