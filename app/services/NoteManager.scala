package services

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.Notes.scope._
import models.activity.ActivityContext
import models.customer.Customers
import models.order.Orders
import models.payment.giftcard.GiftCards
import models.rma.Rmas
import models.{Note, Notes, StoreAdmin}
import responses.AdminNotes
import responses.AdminNotes.Root
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.ModelWithIdParameter
import utils.Slick.DbResult
import utils.Slick.implicits._

object NoteManager {

  def createOrderNote(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    note  ← * <~ Notes.create(Note.forOrder(order.id, author.id, payload))
    _     ← * <~ LogActivity.orderNoteCreated(author, order, note)
  } yield AdminNotes.build(note, author)).runTxn()

  def createGiftCardNote(code: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    response ← * <~ createModelNote(giftCard.id, Note.GiftCard, author, payload)
  } yield response).runTxn()

  def createCustomerNote(customerId: Int, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ createModelNote(customer.id, Note.Customer, author, payload)
  } yield response).runTxn()

  def createRmaNote(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ createModelNote(rma.id, Note.Rma, author, payload)
  } yield response).runTxn()

  private def createModelNote(refId: Int, refType: Note.ReferenceType, author: StoreAdmin,
    payload: payloads.CreateNote)(implicit ec: ExecutionContext, db: Database): DbResultT[Root] = for {
    note ← * <~ Notes.create(Note(
      storeAdminId = author.id,
      referenceId = refId,
      referenceType = refType,
      body = payload.body))
  } yield AdminNotes.build(note, author)

  def updateOrderNote(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {
    order   ← * <~ Orders.mustFindByRefNum(refNum)
    oldNote ← * <~ Notes.findOneByIdAndAdminId(noteId, author.id).mustFindOr(NotFoundFailure404(Note, noteId))
    note    ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
    _       ← * <~ LogActivity.orderNoteUpdated(author, order, oldNote, note)
  } yield AdminNotes.build(note, author)).runTxn()

  def deleteOrderNote(refNum: String, noteId: Int, author: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Unit] = (for {

    order   ← * <~ Orders.mustFindByRefNum(refNum)
    oldNote ← * <~ Notes.findOneByIdAndAdminId(noteId, author.id).mustFindOr(NotFoundFailure404(Note, noteId))
    note    ← * <~ Notes.update(oldNote, oldNote.copy(deletedAt = Some(Instant.now), deletedBy = Some(author.id)))
    _       ← * <~ LogActivity.orderNoteDeleted(author, order, note)
  } yield ()).runTxn()

  def updateGiftCardNote(code: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    _        ← * <~ GiftCards.mustFindByCode(code)
    response ← * <~ updateNote(noteId, author, payload)
  } yield response).runTxn()

  def updateCustomerNote(customerId: Int, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    _        ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ updateNote(noteId, author, payload)
  } yield response).runTxn()

  def updateRmaNote(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    _        ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ updateNote(noteId, author, payload)
  } yield response).runTxn()

  private def updateNote(noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): DbResultT[Root] = for {
    oldNote ← * <~ Notes.filterByIdAndAdminId(noteId, author.id).one.mustFindOr(NotFoundFailure404(Note, noteId))
    newNote ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
  } yield AdminNotes.build(newNote, author)

  def deleteNote(noteId: Int, admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database): Result[Unit] = (for {
    note ← * <~ Notes.mustFindById404(noteId)
    _    ← * <~ Notes.update(note, note.copy(deletedAt = Some(Instant.now), deletedBy = Some(admin.id)))
  } yield {}).runTxn()

  def forOrder(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = (for {
    order    ← * <~ Orders.mustFindByRefNum(refNum)
    response ← * <~ forModel(Notes.filterByOrderId(order.id).notDeleted)
  } yield response).run()

  def forGiftCard(code: String)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    response ← * <~ forModel(Notes.filterByGiftCardId(giftCard.id).notDeleted)
  } yield response).run()

  def forCustomer(customerId: Int)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ forModel(Notes.filterByCustomerId(customer.id).notDeleted)
  } yield response).run()

  def forRma(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ forModel(Notes.filterByRmaId(rma.id).notDeleted)
  } yield response).run()

  private def forModel[M <: ModelWithIdParameter[M]](finder: Notes.QuerySeq)
    (implicit ec: ExecutionContext, db: Database): DbResult[Seq[Root]] = {
    val query = for (notes ← finder; authors ← notes.author) yield (notes, authors)
    DbResult.fromDbio(query.result.map(_.map { case (note, author) ⇒ AdminNotes.build(note, author) }))
  }
}
