package services.notes

import models.Note
import models.objects.{IlluminatedObject, ObjectForms, ObjectShadows}
import models.promotion.Promotions
import utils.aliases._
import utils.db._

object PromotionNoteManager extends NoteManager[Int, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Promotion

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      promotion ← * <~ Promotions.filterByContextAndFormId(defaultContextId, id) ~> Promotions
                   .notFound404(Map("context" → "<default context>", "id" → id))
      form   ← * <~ ObjectForms.findById(promotion.formId)
      shadow ← * <~ ObjectShadows.findById(promotion.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)
}
