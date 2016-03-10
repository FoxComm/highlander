package services.notes

import models.{Note, Notes}
import models.Notes.scope._
import models.activity.ActivityContext
import models.customer.{Customer, Customers}
import utils.Slick._
import utils.aliases._

object CustomerNoteManager extends NoteManager[Int, Customer] {

  def noteType(): Note.ReferenceType = Note.Customer

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: ActivityContext): DbResult[Customer] =
    Customers.mustFindById404(id)

  def entityQuerySeq(entityId: Int)(implicit ec: EC, db: DB, ac: ActivityContext): Notes.QuerySeq =
    Notes.filterByCustomerId(entityId).notDeleted
}
