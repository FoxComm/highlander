package models.cord.lineitems

import models.payment.giftcard.{GiftCard, GiftCards}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class OrderLineItemGiftCard(id: Int = 0, cordRef: String, giftCardId: Int)
    extends FoxModel[OrderLineItemGiftCard]

object OrderLineItemGiftCard {}

class OrderLineItemGiftCards(tag: Tag)
    extends FoxTable[OrderLineItemGiftCard](tag, "order_line_item_gift_cards") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef    = column[String]("cord_ref")
  def giftCardId = column[Int]("gift_card_id")

  def * =
    (id, cordRef, giftCardId) <> ((OrderLineItemGiftCard.apply _).tupled, OrderLineItemGiftCard.unapply)
  def giftCard = foreignKey(GiftCards.tableName, giftCardId, GiftCards)(_.id)
}

object OrderLineItemGiftCards
    extends FoxTableQuery[OrderLineItemGiftCard, OrderLineItemGiftCards](
        new OrderLineItemGiftCards(_))
    with ReturningId[OrderLineItemGiftCard, OrderLineItemGiftCards] {

  val returningLens: Lens[OrderLineItemGiftCard, Int] = lens[OrderLineItemGiftCard].id

  def findByOrderRef(cordRef: Rep[String]): QuerySeq =
    filter(_.cordRef === cordRef)

  def findLineItemsByCordRef(refNum: String) =
    for {
      liGc ← findByOrderRef(refNum)
      gc   ← GiftCards if gc.id === liGc.giftCardId
    } yield (gc, liGc)

  def findByGcId(giftCardId: Int): QuerySeq =
    filter(_.giftCardId === giftCardId)

  object scope {
    implicit class OrderLineItemGiftCardsQuerySeqConversions(q: QuerySeq) {
      def withGiftCards: Query[(OrderLineItemGiftCards, GiftCards),
                               (OrderLineItemGiftCard, GiftCard),
                               Seq] =
        for {
          items     ← q
          giftCards ← items.giftCard
        } yield (items, giftCards)
    }
  }
}
