package phoenix.utils.seeds

import com.github.tminglei.slickpg.LTree
import core.db._
import core.failures.NotFoundFailure404
import phoenix.models.account.Scope
import phoenix.models.admin.{AdminData, AdminsData}
import phoenix.models.cord.{Cord, Cords}
import phoenix.models.payment.giftcard.GiftCard.{buildAppeasement ⇒ build}
import phoenix.models.payment.giftcard._
import phoenix.models.{Note, Notes, Reason, Reasons}
import phoenix.payloads.GiftCardPayloads.{GiftCardCreateByCsr ⇒ payload}
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait GiftCardSeeds {

  def giftCard: GiftCard =
    build(payload(balance = 5000, reasonId = 1), originId = 1, scope = LTree("1.2"))

  def insertCords: DbResultT[Unit] =
    for {
      _ ← * <~ Cords.create(Cord(1, "referenceNumber", true))
    } yield {}

  def createGiftCards(implicit au: AU): DbResultT[Unit] =
    for {
      scope ← * <~ Scope.resolveOverride()
      _     ← * <~ GiftCardSubtypes.createAll(giftCardSubTypes)

      admin ← * <~ AdminsData.mustFindOneOr(NotFoundFailure404(AdminData, "???")) // FIXME: get this ID from an `INSERT`? @michalrus
      reason ← * <~ Reasons
                .filter(_.reasonType === (Reason.GiftCardCreation: Reason.ReasonType))
                .mustFindOneOr(NotFoundFailure404(Reason, "???")) // FIXME: get this ID from an `INSERT`? @michalrusr
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.accountId, reasonId = reason.id))
      gc1    ← * <~ GiftCards.create(giftCard.copy(originId = origin.id))
      _ ← * <~ GiftCards.create(
           build(payload(balance = 10000, reasonId = reason.id), originId = origin.id, scope = scope))
      _ ← * <~ Notes.createAll(
           giftCardNotes.map(_.copy(referenceId = gc1.id, storeAdminId = admin.accountId)))
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
