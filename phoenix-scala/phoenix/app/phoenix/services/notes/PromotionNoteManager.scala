package phoenix.services.notes

import core.db._
import objectframework.models.{IlluminatedObject, ObjectForms, ObjectShadows}
import phoenix.failures.PromotionFailures.PromotionNotFoundForContext
import phoenix.models.Note
import phoenix.models.promotion.Promotions
import phoenix.utils.aliases._

object PromotionNoteManager extends NoteManager[Int, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Promotion

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(defaultContextId, id)
                   .mustFindOneOr(PromotionNotFoundForContext(id, "<default context>"))
      form   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)
}
