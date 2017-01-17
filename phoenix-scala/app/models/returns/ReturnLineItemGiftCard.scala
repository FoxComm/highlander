package models.returns

import java.time.Instant

import models.payment.giftcard.{GiftCard, GiftCards}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class ReturnLineItemGiftCard(id: Int = 0,
                                  returnId: Int,
                                  giftCardId: Int,
                                  createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItemGiftCard]

object ReturnLineItemGiftCard {}

class ReturnLineItemGiftCards(tag: Tag)
    extends FoxTable[ReturnLineItemGiftCard](tag, "return_line_item_gift_cards") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId   = column[Int]("return_id")
  def giftCardId = column[Int]("gift_card_id")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, returnId, giftCardId, createdAt) <> ((ReturnLineItemGiftCard.apply _).tupled, ReturnLineItemGiftCard.unapply)
  def giftCard = foreignKey(GiftCards.tableName, giftCardId, GiftCards)(_.id)
}

object ReturnLineItemGiftCards
    extends FoxTableQuery[ReturnLineItemGiftCard, ReturnLineItemGiftCards](
      new ReturnLineItemGiftCards(_))
    with ReturningId[ReturnLineItemGiftCard, ReturnLineItemGiftCards] {

  val returningLens: Lens[ReturnLineItemGiftCard, Int] = lens[ReturnLineItemGiftCard].id

  def findByRmaId(returnId: Rep[Int]): QuerySeq =
    filter(_.returnId === returnId)

  def findLineItemsByRma(
      rma: Return): Query[(GiftCards, ReturnLineItems), (GiftCard, ReturnLineItem), Seq] =
    for {
      liGc      ← findByRmaId(rma.id)
      li        ← ReturnLineItems if li.originId === liGc.id
      giftCards ← GiftCards if giftCards.id === liGc.giftCardId
    } yield (giftCards, li)
}
