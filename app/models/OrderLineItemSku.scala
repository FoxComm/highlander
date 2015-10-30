package models

import scala.concurrent.ExecutionContext

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class OrderLineItemSku(id: Int = 0, orderId: Int, skuId: Int) extends ModelWithIdParameter[OrderLineItemSku]

object OrderLineItemSku {}

class OrderLineItemSkus(tag: Tag) extends GenericTable.TableWithId[OrderLineItemSku](tag, "order_line_item_skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def skuId = column[Int]("sku_id")

  def * = (id, orderId, skuId) <> ((OrderLineItemSku.apply _).tupled, OrderLineItemSku.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object OrderLineItemSkus extends TableQueryWithId[OrderLineItemSku, OrderLineItemSkus](
  idLens = GenLens[OrderLineItemSku](_.id)
)(new OrderLineItemSkus(_)){

  def findByOrderId(orderId: Rep[Int]): QuerySeq =
    filter(_.orderId === orderId)

  def findLineItemsByOrder(order: Order): Query[(Skus, OrderLineItems), (Sku, OrderLineItem), Seq] = {
    for {
      liSku ← findByOrderId(order.id)
      li ← OrderLineItems if li.originId === liSku.id
      sku ← Skus if sku.id === liSku.skuId
    } yield (sku, li)
  }

  object scope {
    implicit class OrderLineItemSkusQuerySeqConversions(q: QuerySeq) {
      def withSkus: Query[(OrderLineItemSkus, Skus), (OrderLineItemSku, Sku), Seq] = for {
        items ← q
        skus  ← items.sku
      } yield (items, skus)
    }
  }
}
