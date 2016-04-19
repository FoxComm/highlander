package models.order.lineitems

import models.order.Order
import models.payment.giftcard.{GiftCard, GiftCards}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

case class OrderLineItemGiftCard(id: Int = 0, orderId: Int, giftCardId: Int)
  extends ModelWithIdParameter[OrderLineItemGiftCard]

object OrderLineItemGiftCard {}

class OrderLineItemGiftCards(tag: Tag) extends
  GenericTable.TableWithId[OrderLineItemGiftCard](tag, "order_line_item_gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def giftCardId = column[Int]("gift_card_id")

  def * = (id, orderId, giftCardId) <> ((OrderLineItemGiftCard.apply _).tupled, OrderLineItemGiftCard.unapply)
  def giftCard = foreignKey(GiftCards.tableName, giftCardId, GiftCards)(_.id)
}

object OrderLineItemGiftCards extends TableQueryWithId[OrderLineItemGiftCard, OrderLineItemGiftCards](
  idLens = GenLens[OrderLineItemGiftCard](_.id)
)(new OrderLineItemGiftCards(_)){

  def findByOrderId(orderId: Rep[Int]): QuerySeq =
    filter(_.orderId === orderId)

  def findLineItemsByOrder(order: Order) = for {
    liGc ← findByOrderId(order.id)
    gc ← GiftCards if gc.id === liGc.giftCardId
  } yield (gc, liGc)

  def findByGcId(giftCardId: Int): QuerySeq =
    filter(_.giftCardId === giftCardId)

  object scope {
    implicit class OrderLineItemGiftCardsQuerySeqConversions(q: QuerySeq) {
      def withGiftCards: Query[(OrderLineItemGiftCards, GiftCards), (OrderLineItemGiftCard, GiftCard), Seq] = for {
        items     ← q
        giftCards ← items.giftCard
      } yield (items, giftCards)
    }
  }
}
