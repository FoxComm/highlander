package services.notes

import models.Note
import models.order.{Order, Orders}
import utils.Slick._
import utils.aliases._

object OrderNoteManager extends NoteManager[String, Order] {

  def noteType(): Note.ReferenceType = Note.Order

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Order] =
    Orders.mustFindByRefNum(refNum)
}
