package services.notes

import models.product.{Product, Products}
import models.Note
import utils.Slick._
import utils.aliases._

object ProductNoteManager extends NoteManager[Int, Product] {

  def noteType(): Note.ReferenceType = Note.Product

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Product] =
    Products.mustFindById404(id)
}
