package services.notes

import models.Notes.scope._
import models.payment.giftcard.GiftCards
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import responses.AdminNotes
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object GiftCardNoteManager {

  def create(code: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    giftCard  ← * <~ GiftCards.mustFindByCode(code)
    note      ← * <~ createNote(giftCard, giftCard.id, Note.GiftCard, author, payload)
  } yield AdminNotes.build(note, author)).runTxn()

  def update(code: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    note     ← * <~ updateNote(giftCard, noteId, author, payload)
  } yield note).runTxn()

  def delete(code: String, noteId: Int, author: StoreAdmin)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    _        ← * <~ deleteNote(giftCard, noteId, author)
  } yield ()).runTxn()

  def list(code: String)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    giftCard  ← * <~ GiftCards.mustFindByCode(code)
    note      ← * <~ forModel(Notes.filterByGiftCardId(giftCard.id).notDeleted)
  } yield note).run()
}
