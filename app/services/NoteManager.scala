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
import utils.ModelWithIdParameter

object NoteManager {

  def createNote[M <: ModelWithIdParameter](m: M, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = m match {
      case _: Customer ⇒ createModelNote(m.id, Note.Customer, author, payload)
      case _: GiftCard ⇒ createModelNote(m.id, Note.GiftCard, author, payload)
      case _: Order ⇒ createModelNote(m.id, Note.Order, author, payload)
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
