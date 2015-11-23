package services

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.data.Validated.{Invalid, Valid}
import models._
import responses.AdminNotes
import responses.AdminNotes.Root
import slick.driver.PostgresDriver.api._
import services.orders._
import utils.DbResultT.DbResultT
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.ModelWithIdParameter
import models.Notes.scope._

import utils.DbResultT._
import utils.DbResultT.implicits._
import services.orders.Helpers._

object NoteManager {

  def createOrderNote(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    order ← * <~ mustFindOrderByRefNum(refNum)
    note  ← * <~ Notes.create(Note.forOrder(order.id, author.id, payload))
  } yield AdminNotes.build(note, author)).runT()

  def createGiftCardNote(code: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    GiftCards.findByCode(code).selectOne { giftCard ⇒
      createModelNote(giftCard.id, Note.GiftCard, author, payload)
    }
  }

  def createCustomerNote(customerId: Int, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    Customers.findById(customerId).extract.selectOne { customer ⇒
      createModelNote(customer.id, Note.Customer, author, payload)
    }
  }

  def createRmaNote(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    Rmas.findByRefNum(refNum).selectOne { rma ⇒
      createModelNote(rma.id, Note.Rma, author, payload)
    }
  }

  private def createModelNote(refId: Int, refType: Note.ReferenceType, author: StoreAdmin,
    payload: payloads.CreateNote)(implicit ec: ExecutionContext, db: Database): DbResult[Root] = {
    createNote(Note(
      storeAdminId = author.id,
      referenceId = refId,
      referenceType = refType,
      body = payload.body)
    ).map(_.map(AdminNotes.build(_, author)))
  }

  def updateOrderNote(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    Orders.findByRefNum(refNum).selectOne { _ ⇒ updateNote(noteId, author, payload) }
  }

  def updateGiftCardNote(code: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    GiftCards.findByCode(code).selectOne { _ ⇒ updateNote(noteId, author, payload) }
  }

  def updateCustomerNote(customerId: Int, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    Customers.findById(customerId).extract.selectOne { _ ⇒ updateNote(noteId, author, payload) }
  }

  def updateRmaNote(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    Rmas.findByRefNum(refNum).selectOne { _ ⇒ updateNote(noteId, author, payload) }
  }

  private def updateNote(noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): DbResult[Root] = {

    val finder = Notes.filterByIdAndAdminId(noteId, author.id)
    val update = finder.map(_.body).update(payload.body)

    update.flatMap { rowsAffected ⇒
      if (rowsAffected == 1) {
        finder.one.flatMap {
          case Some(note) ⇒ DbResult.good(AdminNotes.build(note, author))
          case None       ⇒ DbResult.failure(notFound(noteId))
        }
      } else {
        DbResult.failure(notFound(noteId))
      }
    }
  }

  private def notFound(noteId: Int): NotFoundFailure404 = NotFoundFailure404(Note, noteId)

  def deleteNote(noteId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    val finder = Notes.findById(noteId).extract

    finder.selectOneForUpdate { note ⇒
      finder.map(n ⇒ (n.deletedAt, n.deletedBy)).update((Some(Instant.now), Some(admin.id))) >> DbResult.unit
    }
  }

  private def createNote(note: Note)
    (implicit ec: ExecutionContext, db: Database): DbResult[Note] = {
    note.validate match {
      case Valid(_)         ⇒ DbResult.fromDbio(Notes.saveNew(note))
      case Invalid(errors)  ⇒ DbResult.failures(errors)
    }
  }

  def forOrder(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    Orders.findByRefNum(refNum).selectOne { order ⇒
      forModel(Notes.filterByOrderId(order.id).notDeleted)
    }
  }

  def forGiftCard(code: String)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    GiftCards.findByCode(code).selectOne { giftCard ⇒
      forModel(Notes.filterByGiftCardId(giftCard.id).notDeleted)
    }
  }

  def forCustomer(customerId: Int)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    Customers.findById(customerId).extract.selectOne { customer ⇒
      forModel(Notes.filterByCustomerId(customer.id).notDeleted)
    }
  }

  def forRma(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    Rmas.findByRefNum(refNum).selectOne { rma ⇒
      forModel(Notes.filterByRmaId(rma.id).notDeleted)
    }
  }

  private def forModel[M <: ModelWithIdParameter[M]](finder: Notes.QuerySeq)
    (implicit ec: ExecutionContext, db: Database): DbResult[Seq[Root]] = {
    val q = for {
      notes ← finder
      authors ← notes.author
    } yield (notes, authors)

    val notes = q.result.map { _.map {
        case (note, author) ⇒ AdminNotes.build(note, author)
      }
    }

    DbResult.fromDbio(notes)
  }
}
