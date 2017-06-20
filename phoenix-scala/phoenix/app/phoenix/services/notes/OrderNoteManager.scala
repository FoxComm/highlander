package phoenix.services.notes

import core.db._
import phoenix.models.Note
import phoenix.models.cord.{Cord, Cords}
import phoenix.utils.aliases._

object CordNoteManager extends NoteManager[String, Cord] {

  def noteType(): Note.ReferenceType = Note.Order

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Cord] =
    Cords.mustFindByRefNum(refNum)
}
