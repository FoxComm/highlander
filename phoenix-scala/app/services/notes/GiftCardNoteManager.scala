package services.notes

import models.payment.giftcard.{GiftCard, GiftCards}
import models.Note
import utils.aliases._
import utils.db._

object GiftCardNoteManager extends NoteManager[String, GiftCard] {

  def noteType(): Note.ReferenceType = Note.GiftCard

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[GiftCard] =
    GiftCards.mustFindByCode(code)
}
