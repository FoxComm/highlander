package phoenix.services.notes

import phoenix.models.Note
import models.objects._
import phoenix.services.inventory.SkuManager
import phoenix.utils.aliases._
import utils.db._

object SkuNoteManager extends NoteManager[String, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Sku

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndCode(defaultContextId, code)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)
}
