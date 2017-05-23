package phoenix.services.notes

import phoenix.models.returns._
import phoenix.models.Note
import phoenix.utils.aliases._
import utils.db._

object ReturnNoteManager extends NoteManager[String, Return] {

  def noteType(): Note.ReferenceType = Note.Return

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Return] =
    Returns.mustFindByRefNum(refNum)
}
