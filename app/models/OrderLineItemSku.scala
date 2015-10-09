package models

import scala.concurrent.ExecutionContext

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class OrderLineItemSku(id: Int = 0, orderId: Int, skuId: Int) extends ModelWithIdParameter

object OrderLineItemSku {}

class OrderLineItemSkus(tag: Tag) extends GenericTable.TableWithId[OrderLineItemSku](tag, "order_line_item_skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def skuId = column[Int]("sku_id")

  def * = (id, orderId, skuId) <> ((OrderLineItemSku.apply _).tupled, OrderLineItemSku.unapply)
}

object OrderLineItemSkus extends TableQueryWithId[OrderLineItemSku, OrderLineItemSkus](
  idLens = GenLens[OrderLineItemSku](_.id)
)(new OrderLineItemSkus(_)){

  def findByOrderId(orderId: Rep[Int]): Query[OrderLineItemSkus, OrderLineItemSku, Seq] =
    filter(_.orderId === orderId)

  def findLineItemsByOrder(order: Order) = {
    for {
      liSku ← findByOrderId(order.id)
      li ← OrderLineItems if li.originId === liSku.id
      sku ← Skus if sku.id === liSku.skuId
    } yield (sku, li)
  }

}
