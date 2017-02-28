package services.notes

import models.Note
import models.objects._
import services.inventory.SkuManager
import utils.aliases._
import utils.db._

object SkuNoteManager extends NoteManager[String, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Sku

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndCode(defaultContextId, code)
      form   ← * <~ ObjectForms.findById(sku.formId)
      shadow ← * <~ ObjectShadows.findById(sku.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)
}
