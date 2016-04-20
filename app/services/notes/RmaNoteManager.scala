package services.notes

import models.rma.{Rma, Rmas}
import models.Note
import utils.aliases._
import utils.db._

object RmaNoteManager extends NoteManager[String, Rma] {

  def noteType(): Note.ReferenceType = Note.Rma

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Rma] =
    Rmas.mustFindByRefNum(refNum)
}
