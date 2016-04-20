package services.notes

import models.Note
import models.customer.{Customer, Customers}
import utils.aliases._
import utils.db._

object CustomerNoteManager extends NoteManager[Int, Customer] {

  def noteType(): Note.ReferenceType = Note.Customer

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Customer] =
    Customers.mustFindById404(id)
}
