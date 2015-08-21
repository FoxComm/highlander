package services

import models._
import responses.AdminNotes
import responses.AdminNotes.Root
import slick.dbio.Effect.Write
import slick.profile.FixedSqlAction
import utils.Validation.Result._

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

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
        db.run(query.result.headOption).map {
          case Some(note) ⇒ Good(AdminNotes.build(note, author))
          case None       ⇒ Bad(notFound(noteId).single)
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
      case Success ⇒
        Notes.save(note).run().map(Good(_))
      case f @ Failure(_) ⇒
        Future.successful(Bad(ValidationFailure(f).single))
    }
  }
}
