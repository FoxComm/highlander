package services.notes

import models.payment.giftcard.{GiftCard, GiftCards}
import models.Note
import utils.Slick._
import utils.aliases._

object GiftCardNoteManager extends NoteManager[String, GiftCard] {

  def noteType(): Note.ReferenceType = Note.GiftCard

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResult[GiftCard] =
    GiftCards.mustFindByCode(code)
}
