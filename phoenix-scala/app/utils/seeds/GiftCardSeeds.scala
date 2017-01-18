package utils.seeds

import models.cord.{Cord, Cords}

import com.github.tminglei.slickpg.LTree
import models.account.Scope
import scala.concurrent.ExecutionContext.Implicits.global
import models.payment.giftcard.GiftCard.{buildAppeasement ⇒ build}
import models.payment.giftcard._
import models.{Note, Notes}
import payloads.GiftCardPayloads.{GiftCardCreateByCsr ⇒ payload}
import utils.aliases._
import utils.db._

trait GiftCardSeeds {

  def giftCard: GiftCard =
    build(payload(balance = 5000, reasonId = 1), originId = 1, scope = LTree("1.2"))

  def insertCords: DbResultT[Unit] =
    for {
      _ ← * <~ Cords.create(Cord(1, "referenceNumber", true))
    } yield {}

  def createGiftCards(implicit au: AU): DbResultT[Unit] =
    for {
      scope  ← * <~ Scope.resolveOverride()
      _      ← * <~ GiftCardSubtypes.createAll(giftCardSubTypes)
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
      gc1    ← * <~ GiftCards.create(giftCard.copy(originId = origin.id))
      _      ← * <~ GiftCards.capture(gc1, debit = 1000, orderPaymentId = None)
      _ ← * <~ GiftCards.create(
        build(payload(balance = 10000, reasonId = 1), originId = origin.id, scope = scope))
      _ ← * <~ Notes.createAll(giftCardNotes.map(_.copy(referenceId = gc1.id)))
    } yield {}

  def giftCardSubTypes: Seq[GiftCardSubtype] = Seq(
    GiftCardSubtype(title = "Appeasement Subtype A", originType = GiftCard.CsrAppeasement),
    GiftCardSubtype(title = "Appeasement Subtype B", originType = GiftCard.CsrAppeasement),
    GiftCardSubtype(title = "Appeasement Subtype C", originType = GiftCard.CsrAppeasement)
  )

  def giftCardNotes(implicit au: AU): Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1,
           referenceType = Note.GiftCard,
           storeAdminId = 1,
           body = body,
           scope = Scope.current)
    Seq(
      newNote("This customer is a donkey."),
      newNote("No, seriously."),
      newNote("Like, an actual donkey."),
      newNote("How did a donkey even place an order on our website?")
    )
  }
}
