package services.notes

import models.payment.giftcard.{GiftCard, GiftCards}
import models.{Note, Notes}
import models.Notes.scope._
import models.activity.ActivityContext
import utils.Slick._
import utils.aliases._

object GiftCardNoteManager extends NoteManager[String, GiftCard] {

  def noteType(): Note.ReferenceType = Note.GiftCard

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: ActivityContext): DbResult[GiftCard] =
    GiftCards.mustFindByCode(code)

  def entityQuerySeq(entityId: Int)(implicit ec: EC, db: DB, ac: ActivityContext): Notes.QuerySeq =
    Notes.filterByGiftCardId(entityId).notDeleted
}
