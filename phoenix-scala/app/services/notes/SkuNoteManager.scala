package services.notes

import models.Note
import models.objects._
import services.inventory.SkuManager
import utils.aliases._
import utils.db._

object SkuNoteManager extends NoteManager[Int, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Sku

  def fetchEntity(skuId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndId(defaultContextId, skuId)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield IlluminatedObject.illuminate(form = form, shadow = shadow)
}
