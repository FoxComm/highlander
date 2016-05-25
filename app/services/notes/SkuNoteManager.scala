package services.notes

import models.inventory.{Sku, Skus}
import models.Note
import models.objects.{ObjectForm, ObjectForms}
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object SkuNoteManager extends NoteManager[String, ObjectForm] {

  def noteType(): Note.ReferenceType = Note.Sku

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResult[ObjectForm] =
    (for {
      sku  ← * <~ Skus.mustFindByCode(code)
      form ← * <~ ObjectForms.mustFindById404(sku.formId)
    } yield form).value
}
