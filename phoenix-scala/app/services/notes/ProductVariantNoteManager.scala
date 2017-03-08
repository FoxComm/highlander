package services.notes

import models.Note
import models.objects._
import services.inventory.ProductVariantManager
import utils.aliases._
import utils.db._

object ProductVariantNoteManager extends NoteManager[String, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Variant

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      variant ← * <~ ProductVariantManager.mustFindByContextAndCode(defaultContextId, code)
      form    ← * <~ ObjectForms.mustFindById404(variant.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(variant.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)
}
