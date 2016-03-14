package services.notes

import models.inventory.{Sku, Skus}
import models.Note
import utils.Slick._
import utils.aliases._

object SkuNoteManager extends NoteManager[String, Sku] {

  def noteType(): Note.ReferenceType = Note.Sku

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Sku] =
    Skus.mustFindByCode(code)
}
