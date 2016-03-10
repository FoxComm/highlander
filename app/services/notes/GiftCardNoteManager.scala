package services.notes

import models.Notes.scope._
import models.payment.giftcard.GiftCards
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object GiftCardNoteManager {

  def createGiftCardNote(code: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    response ← * <~ createModelNote(giftCard.id, Note.GiftCard, author, payload)
  } yield response).runTxn()

  def updateGiftCardNote(code: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    _        ← * <~ GiftCards.mustFindByCode(code)
    response ← * <~ updateNote(noteId, author, payload)
  } yield response).runTxn()

  def deleteGiftCardNote(noteId: Int, author: StoreAdmin)(implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] =
    deleteNote(noteId, author)

  def forGiftCard(code: String)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    response ← * <~ forModel(Notes.filterByGiftCardId(giftCard.id).notDeleted)
  } yield response).run()
}
