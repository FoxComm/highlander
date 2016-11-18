package models.payment.giftcard

import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.aliases.Json
import utils.db._

case class GiftCardOrder(id: Int = 0, cordRef: String, details: Option[Json] = None)
    extends FoxModel[GiftCardOrder]

object GiftCardOrder {}

class GiftCardOrders(tag: Tag) extends FoxTable[GiftCardOrder](tag, "gift_card_orders") {
  def id      = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef = column[String]("cord_ref")
  def details = column[Option[Json]]("details")

  def * = (id, cordRef, details) <> ((GiftCardOrder.apply _).tupled, GiftCardOrder.unapply)
}

object GiftCardOrders
    extends FoxTableQuery[GiftCardOrder, GiftCardOrders](new GiftCardOrders(_))
    with ReturningId[GiftCardOrder, GiftCardOrders] {
  val returningLens: Lens[GiftCardOrder, Int] = lens[GiftCardOrder].id
}
