package phoenix.services.notes

import phoenix.models.payment.giftcard.{GiftCard, GiftCards}
import phoenix.models.Note
import phoenix.utils.aliases._
import utils.db._

object GiftCardNoteManager extends NoteManager[String, GiftCard] {

  def noteType(): Note.ReferenceType = Note.GiftCard

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[GiftCard] =
    GiftCards.mustFindByCode(code)
}
