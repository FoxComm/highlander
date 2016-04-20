package models.payment.giftcard

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.db._

case class GiftCardOrder(id: Int = 0, orderId: Int) extends FoxModel[GiftCardOrder]

object GiftCardOrder {}

class GiftCardOrders(tag: Tag) extends FoxTable[GiftCardOrder](tag, "gift_card_orders")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")

  def * = (id, orderId) <> ((GiftCardOrder.apply _).tupled, GiftCardOrder.unapply)
}

object GiftCardOrders extends FoxTableQuery[GiftCardOrder, GiftCardOrders](
  idLens = GenLens[GiftCardOrder](_.id)
  )(new GiftCardOrders(_)){
}
