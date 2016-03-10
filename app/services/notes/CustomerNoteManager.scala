package services.notes

import models.Notes.scope._
import models.customer.Customers
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import responses.AdminNotes
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object CustomerNoteManager {

  def create(customerId: Int, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    customer  ← * <~ Customers.mustFindById404(customerId)
    note      ← * <~ createModelNote(customer.id, Note.Customer, author, payload)
    _         ← * <~ LogActivity.noteCreated(author, customer, note)
  } yield AdminNotes.build(note, author)).runTxn()

  def update(customerId: Int, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    note     ← * <~ updateNote(customer, noteId, author, payload)
  } yield note).runTxn()

  def delete(customerId: Int, noteId: Int, author: StoreAdmin)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] = (for {
    customer  ← * <~ Customers.mustFindById404(customerId)
    _         ← * <~ deleteNote(customer, noteId, author)
  } yield ()).runTxn()

  def list(customerId: Int)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    customer  ← * <~ Customers.mustFindById404(customerId)
    note      ← * <~ forModel(Notes.filterByCustomerId(customer.id).notDeleted)
  } yield note).run()
}
