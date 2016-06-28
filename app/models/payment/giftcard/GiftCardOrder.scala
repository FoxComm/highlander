package models.payment.giftcard

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class GiftCardOrder(id: Int = 0, orderRef: String) extends FoxModel[GiftCardOrder]

object GiftCardOrder {}

class GiftCardOrders(tag: Tag) extends FoxTable[GiftCardOrder](tag, "gift_card_orders") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderRef = column[String]("order_ref")

  def * = (id, orderRef) <> ((GiftCardOrder.apply _).tupled, GiftCardOrder.unapply)
}

object GiftCardOrders
    extends FoxTableQuery[GiftCardOrder, GiftCardOrders](new GiftCardOrders(_))
    with ReturningId[GiftCardOrder, GiftCardOrders] {
  val returningLens: Lens[GiftCardOrder, Int] = lens[GiftCardOrder].id
}
