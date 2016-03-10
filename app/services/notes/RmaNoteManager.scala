package services.notes

import models.rma.{Rma, Rmas}
import models.{Note, Notes}
import models.Notes.scope._
import models.activity.ActivityContext
import utils.Slick._
import utils.aliases._

object RmaNoteManager extends NoteManager[String, Rma] {

  def noteType(): Note.ReferenceType = Note.Rma

  def fetchEntity(refNum: String)(implicit ec: EC, db: DB, ac: ActivityContext): DbResult[Rma] =
    Rmas.mustFindByRefNum(refNum)

  def entityQuerySeq(entityId: Int)(implicit ec: EC, db: DB, ac: ActivityContext): Notes.QuerySeq =
    Notes.filterByOrderId(entityId).notDeleted
}
