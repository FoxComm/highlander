package services.notes

import failures.NotFoundFailure404
import models.Note
import models.objects.{ObjectForm, ObjectForms}
import models.promotion.Promotion
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object PromotionNoteManager extends NoteManager[Int, ObjectForm] {

  def noteType(): Note.ReferenceType = Note.Promotion

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[ObjectForm] =
    ObjectForms.filter(_.kind === ObjectForm.promotion).one.mustFindOr(NotFoundFailure404(Promotion, id))
}
