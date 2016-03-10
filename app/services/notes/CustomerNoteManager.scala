package services.notes

import models.Notes.scope._
import models.customer.Customers
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object CustomerNoteManager {

  def createCustomerNote(customerId: Int, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ createModelNote(customer.id, Note.Customer, author, payload)
  } yield response).runTxn()

  def updateCustomerNote(customerId: Int, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    _        ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ updateNote(noteId, author, payload)
  } yield response).runTxn()

  def deleteCustomerNote(noteId: Int, author: StoreAdmin)(implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] =
    deleteNote(noteId, author)

  def forCustomer(customerId: Int)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ forModel(Notes.filterByCustomerId(customer.id).notDeleted)
  } yield response).run()
}
