package services

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.data.Validated.{Invalid, Valid}
import models._
import responses.AdminNotes
import responses.AdminNotes.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.implicits._

object NoteManager {

  def createOrderNote(order: Order, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    createModelNote(order.id, Note.Order, author, payload)
  }

  def createGiftCardNote(giftCard: GiftCard, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    createModelNote(giftCard.id, Note.GiftCard, author, payload)
  }

  private def createModelNote(refId: Int, refType: Note.ReferenceType, author: StoreAdmin,
    payload: payloads.CreateNote)(implicit ec: ExecutionContext, db: Database): Result[Root] = {
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

  private def notFound(noteId: Int): NotFoundFailure = NotFoundFailure(Note, noteId)

  def deleteNote(noteId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    val finder = Notes.findById(noteId).extract

    finder.selectOneForUpdate { note ⇒
      finder.map(n ⇒ (n.deletedAt, n.deletedBy)).update((Some(Instant.now), Some(admin.id))) >> DbResult.unit
    }
  }

  private def createNote(note: Note)
    (implicit ec: ExecutionContext, db: Database): Result[Note] = {
    note.validate match {
      case Valid(_)         ⇒ Result.fromFuture(Notes.save(note).run())
      case Invalid(errors)  ⇒ Result.failures(errors)
    }
  }
}
