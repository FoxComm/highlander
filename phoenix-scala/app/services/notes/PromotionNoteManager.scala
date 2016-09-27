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

  def getEntityId(e: ObjectForm): Int = e.id

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[ObjectForm] =
    ObjectForms
      .filter(_.id === id)
      .filter(_.kind === ObjectForm.promotion)
      .mustFindOneOr(NotFoundFailure404(Promotion, id))
}
