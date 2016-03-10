package services.notes

import java.time.Instant

import models.Notes.scope._
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import models.order.Orders
import responses.AdminNotes
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.aliases._

object OrderNoteManager {

  def createOrderNote(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    note  ← * <~ Notes.create(Note.forOrder(order.id, author.id, payload))
    _     ← * <~ LogActivity.orderNoteCreated(author, order, note)
  } yield AdminNotes.build(note, author)).runTxn()

  def updateOrderNote(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    order   ← * <~ Orders.mustFindByRefNum(refNum)
    oldNote ← * <~ Notes.findOneByIdAndAdminId(noteId, author.id).mustFindOr(NotFoundFailure404(Note, noteId))
    note    ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
    _       ← * <~ LogActivity.orderNoteUpdated(author, order, oldNote, note)
  } yield AdminNotes.build(note, author)).runTxn()

  def deleteOrderNote(refNum: String, noteId: Int, author: StoreAdmin)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] = (for {
    order   ← * <~ Orders.mustFindByRefNum(refNum)
    oldNote ← * <~ Notes.findOneByIdAndAdminId(noteId, author.id).mustFindOr(NotFoundFailure404(Note, noteId))
    note    ← * <~ Notes.update(oldNote, oldNote.copy(deletedAt = Some(Instant.now), deletedBy = Some(author.id)))
    _       ← * <~ LogActivity.orderNoteDeleted(author, order, note)
  } yield ()).runTxn()

  def forOrder(refNum: String)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    order    ← * <~ Orders.mustFindByRefNum(refNum)
    response ← * <~ forModel(Notes.filterByOrderId(order.id).notDeleted)
  } yield response).run()
}
