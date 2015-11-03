package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class GiftCardOrder(id: Int = 0, orderId: Int) extends ModelWithIdParameter[GiftCardOrder]

object GiftCardOrder {}

class GiftCardOrders(tag: Tag) extends GenericTable.TableWithId[GiftCardOrder](tag, "gift_card_orders")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")

  def * = (id, orderId) <> ((GiftCardOrder.apply _).tupled, GiftCardOrder.unapply)
}

object GiftCardOrders extends TableQueryWithId[GiftCardOrder, GiftCardOrders](
  idLens = GenLens[GiftCardOrder](_.id)
  )(new GiftCardOrders(_)){
}
