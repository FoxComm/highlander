package services.notes

import models.Note
import models.cord.{Order, Orders}
import utils.aliases._
import utils.db._

object OrderNoteManager extends NoteManager[String, Order] {

  def noteType(): Note.ReferenceType = Note.Order

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Order] =
    Orders.mustFindByRefNum(refNum)
}
