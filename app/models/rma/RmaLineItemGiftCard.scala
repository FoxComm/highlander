package models.rma

import java.time.Instant

import models.payment.giftcard.{GiftCard, GiftCards}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class RmaLineItemGiftCard(id: Int = 0, rmaId: Int, giftCardId: Int, createdAt: Instant = Instant.now)
  extends FoxModel[RmaLineItemGiftCard]

object RmaLineItemGiftCard {}

class RmaLineItemGiftCards(tag: Tag) extends FoxTable[RmaLineItemGiftCard](tag, "rma_line_item_gift_cards") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def giftCardId = column[Int]("gift_card_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, giftCardId, createdAt) <> ((RmaLineItemGiftCard.apply _).tupled, RmaLineItemGiftCard.unapply)
  def giftCard = foreignKey(GiftCards.tableName, giftCardId, GiftCards)(_.id)
}

object RmaLineItemGiftCards extends FoxTableQuery[RmaLineItemGiftCard, RmaLineItemGiftCards](
  idLens = lens[RmaLineItemGiftCard].id
)(new RmaLineItemGiftCards(_)){

  def findByRmaId(rmaId: Rep[Int]): QuerySeq =
    filter(_.rmaId === rmaId)

  def findLineItemsByRma(rma: Rma): Query[(GiftCards, RmaLineItems), (GiftCard, RmaLineItem), Seq] = for {
    liGc ← findByRmaId(rma.id)
    li ← RmaLineItems if li.originId === liGc.id
    giftCards ← GiftCards if giftCards.id === liGc.giftCardId
  } yield (giftCards, li)
}
