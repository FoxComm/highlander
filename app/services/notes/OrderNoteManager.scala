package services.notes

import models.Notes.scope._
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import models.order.Orders
import responses.AdminNotes
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object OrderNoteManager {

  def create(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    note  ← * <~ createNote(order, order.id, Note.Order, author, payload)
  } yield AdminNotes.build(note, author)).runTxn()

  def update(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    note  ← * <~ updateNote(order, noteId, author, payload)
  } yield note).runTxn()

  def delete(refNum: String, noteId: Int, author: StoreAdmin)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ deleteNote(order, noteId, author)
  } yield ()).runTxn()

  def list(refNum: String)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    order    ← * <~ Orders.mustFindByRefNum(refNum)
    response ← * <~ forModel(Notes.filterByOrderId(order.id).notDeleted)
  } yield response).run()
}
