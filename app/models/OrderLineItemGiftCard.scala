package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class OrderLineItemGiftCard(id: Int = 0, giftCardId: Int) extends ModelWithIdParameter

object OrderLineItemGiftCard {}

class OrderLineItemGiftCards(tag: Tag) extends
  GenericTable.TableWithId[OrderLineItemGiftCard](tag, "order_line_item_gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")

  def * = (id, giftCardId) <> ((OrderLineItemGiftCard.apply _).tupled, OrderLineItemGiftCard.unapply)
}

object OrderLineItemGiftCards extends TableQueryWithId[OrderLineItemGiftCard, OrderLineItemGiftCards](
  idLens = GenLens[OrderLineItemGiftCard](_.id)
)(new OrderLineItemGiftCards(_)){
}
