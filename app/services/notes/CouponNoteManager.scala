package services.notes

import failures.NotFoundFailure404
import models.Note
import models.objects.{ObjectForm, ObjectForms}
import models.coupon.Coupon
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.aliases._

object CouponNoteManager extends NoteManager[Int, ObjectForm] {

  def noteType(): Note.ReferenceType = Note.Coupon

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[ObjectForm] =
    ObjectForms.filter(_.kind === ObjectForm.coupon).one.mustFindOr(NotFoundFailure404(Coupon, id))
}
