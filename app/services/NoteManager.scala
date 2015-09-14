package services

import java.time.Instant

import cats.data.Validated.{Valid, Invalid}
import models._
import responses.AdminNotes
import responses.AdminNotes.Root
import utils.ModelWithIdParameter
import utils.Slick.DbResult
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._

import utils.time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

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

  def updateNote(noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val query = Notes.filterByIdAndAdminId(noteId, author.id)
    val update = query.map(_.body).update(payload.body)

    db.run(update).flatMap { rowsAffected ⇒
      if (rowsAffected == 1) {
        db.run(query.one).flatMap {
          case Some(note) ⇒ Result.right(AdminNotes.build(note, author))
          case None       ⇒ Result.failure(notFound(noteId))
        }
      } else {
        Result.failure(notFound(noteId))
      }
    }
  }

  private def notFound(noteId: Int): NotFoundFailure = NotFoundFailure(Note, noteId)

  def deleteNote(noteId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    Notes._findById(noteId).extract.findOneAndRun { note ⇒
      Notes.update(note.copy(
        deletedAt = Some(Instant.now),
        deletedBy = Some(admin.id))
      ) >> DbResult.unit
    }
  }

  private def createNote(note: Note)
    (implicit ec: ExecutionContext, db: Database): Result[Note] = {
    note.validate match {
      case Valid(_)         ⇒ Result.fromFuture(Notes.save(note).run())
      case Invalid(errors)  ⇒ Result.failure(errors.head)
    }
  }
}
