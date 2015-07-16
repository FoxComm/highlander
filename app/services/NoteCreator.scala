package services

import models._
import utils.Validation.Result._
import payloads.UpdateOrderPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._

object NoteCreator {

  def createOrderNote(order: Order, admin: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: ExecutionContext, db: Database) = {
    createNote(Note(storeAdminId = admin.id, referenceId = order.id, referenceType = Note.Order,
      body = payload.body))
  }

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
