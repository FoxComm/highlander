package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.GiftCard.{buildAppeasement ⇒ build}
import models.{GiftCard, GiftCardManual, GiftCardManuals, GiftCardSubtype, GiftCardSubtypes, GiftCards}
import payloads.{GiftCardCreateByCsr ⇒ payload}
import utils.DbResultT._
import utils.DbResultT.implicits._

trait GiftCardSeeds {

  def giftCard: GiftCard = build(payload(balance = 5000, reasonId = 1), originId = 1)

  def createGiftCards: DbResultT[Unit] = for {
    _      ← * <~ GiftCardSubtypes.createAll(giftCardSubTypes)
    origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
    gc1    ← * <~ GiftCards.create(giftCard.copy(originId = origin.id))
    _      ← * <~ GiftCards.capture(gc1, debit = 1000, orderPaymentId = None)

    gc2    ← * <~ GiftCards.create(build(payload(balance = 10000, reasonId = 1), originId = origin.id))
  } yield {}

  def giftCardSubTypes: Seq[GiftCardSubtype] = Seq(
    GiftCardSubtype(title = "Appeasement Subtype A", originType = GiftCard.CsrAppeasement),
    GiftCardSubtype(title = "Appeasement Subtype B", originType = GiftCard.CsrAppeasement),
    GiftCardSubtype(title = "Appeasement Subtype C", originType = GiftCard.CsrAppeasement)
  )

}
