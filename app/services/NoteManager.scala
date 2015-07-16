package services

import models._
import responses.AdminNotes
import responses.AdminNotes.Root
import utils.Validation.Result._
import payloads.UpdateOrderPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._

object NoteManager {

  def createOrderNote(order: Order, admin: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database): Future[Root Or ValidationFailure] = {
    createNote(Note(storeAdminId = admin.id, referenceId = order.id, referenceType = Note.Order,
      body = payload.body)).map { result ⇒
      result.map(AdminNotes.build(_, admin))
    }
  }

//  def deleteNote(noteId: Int, admin: StoreAdmin)
//    (implicit ec: ExecutionContext, db: Database): Future[Int] = {
//    db.run(Notes._findById(noteId).delete)
//  }

  private def createNote(note: Note)
    (implicit ec: ExecutionContext, db: Database): Future[Note Or ValidationFailure] = {
    note.validate match {
      case Success ⇒
        Notes.save(note).run().map(Good(_))
      case f @ Failure(_) ⇒
        Future.successful(Bad(ValidationFailure(f)))
    }
  }
}
