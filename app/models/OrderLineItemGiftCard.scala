package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class OrderLineItemGiftCard(id: Int = 0, orderId: Int, giftCardId: Int) extends ModelWithIdParameter

object OrderLineItemGiftCard {}

class OrderLineItemGiftCards(tag: Tag) extends
  GenericTable.TableWithId[OrderLineItemGiftCard](tag, "order_line_item_gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def giftCardId = column[Int]("gift_card_id")

  def * = (id, orderId, giftCardId) <> ((OrderLineItemGiftCard.apply _).tupled, OrderLineItemGiftCard.unapply)
}

object OrderLineItemGiftCards extends TableQueryWithId[OrderLineItemGiftCard, OrderLineItemGiftCards](
  idLens = GenLens[OrderLineItemGiftCard](_.id)
)(new OrderLineItemGiftCards(_)){

  def _findByOrderId(orderId: Rep[Int]) = { filter(_.orderId === orderId) }
}
