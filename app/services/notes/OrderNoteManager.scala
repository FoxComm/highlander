package services.notes

import models.{Note, Notes}
import models.Notes.scope._
import models.activity.ActivityContext
import models.order.{Order, Orders}
import utils.Slick._
import utils.aliases._

object OrderNoteManager extends NoteManager[String, Order] {

  def noteType(): Note.ReferenceType = Note.Order

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: ActivityContext): DbResult[Order] =
    Orders.mustFindByRefNum(refNum)

  def entityQuerySeq(entityId: Int)(implicit ec: EC, db: DB, ac: ActivityContext): Notes.QuerySeq =
    Notes.filterByOrderId(entityId).notDeleted
}
