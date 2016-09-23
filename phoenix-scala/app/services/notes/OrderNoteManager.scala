package services.notes

import models.Note
import models.cord.{Cord, Cords}
import utils.aliases._
import utils.db._

object CordNoteManager extends NoteManager[String, Cord] {

  def noteType(): Note.ReferenceType = Note.Cord

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Cord] =
    Cords.mustFindByRefNum(refNum)
}
