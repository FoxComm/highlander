package services.notes

import models.Note
import models.objects.{ObjectForm, ObjectForms}
import utils.Slick._
import utils.aliases._

object ProductNoteManager extends NoteManager[Int, ObjectForm] {

  def noteType(): Note.ReferenceType = Note.Product

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[ObjectForm] =
    ObjectForms.mustFindById404(id)
}
