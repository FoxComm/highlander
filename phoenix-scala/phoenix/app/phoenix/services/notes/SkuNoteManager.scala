package phoenix.services.notes

import objectframework.models._
import phoenix.models.Note
import phoenix.services.inventory.SkuManager
import phoenix.utils.aliases._
import core.db._

object SkuNoteManager extends NoteManager[String, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Sku

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndCode(defaultContextId, code)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)
}
