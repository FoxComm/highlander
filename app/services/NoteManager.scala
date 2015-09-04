package services

import cats.data.Validated.{Valid, Invalid}
import cats.data.Xor
import models._
import responses.AdminNotes
import responses.AdminNotes.Root
import slick.dbio.Effect.Write
import slick.profile.FixedSqlAction

import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._

object NoteManager {

  def createOrderNote(order: Order, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    createNote(Note(storeAdminId = author.id, referenceId = order.id, referenceType = Note.Order,
      body = payload.body)).map { result ⇒
      result.map(AdminNotes.build(_, author))
    }
  }

  def updateNote(noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val query = Notes._filterByIdAndAdminId(noteId, author.id)
    val update = query.map(_.body).update(payload.body)

    db.run(update).flatMap { rowsAffected ⇒
      if (rowsAffected == 1) {
        db.run(query.result.headOption).flatMap {
          case Some(note) ⇒ Result.right(AdminNotes.build(note, author))
          case None       ⇒ Result.failure(notFound(noteId))
        }
      } else {
        Result.failure(notFound(noteId))
      }
    }
  }

  private def notFound(noteId: Int): NotFoundFailure = NotFoundFailure(Note, noteId)

//  def deleteNote(noteId: Int, admin: StoreAdmin)
//    (implicit ec: ExecutionContext, db: Database): Future[Int] = {
//    db.run(Notes._findById(noteId).delete)
//  }

  private def createNote(note: Note)
    (implicit ec: ExecutionContext, db: Database): Result[Note] = {
    note.validate match {
      case Valid(_)         ⇒ Result.fromFuture(Notes.save(note).run())
      case Invalid(errors)  ⇒ Result.failure(errors.head)
    }
  }
}
