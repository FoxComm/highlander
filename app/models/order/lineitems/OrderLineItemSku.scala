package models.order.lineitems

import scala.concurrent.ExecutionContext

import models.inventory.{Skus, Sku, SkuShadow, SkuShadows}
import models.order.Order
import models.product.{Product, Products, ProductShadow, ProductShadows}
import utils.Money.Currency
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}


final case class OrderLineItemSku(id: Int = 0, skuId: Int, skuShadowId: Int)
  extends ModelWithIdParameter[OrderLineItemSku]

final case class OrderLineItemProductData(
  sku: Sku, 
  skuShadow: SkuShadow, 
  lineItem: OrderLineItem)

object OrderLineItemSku {}

class OrderLineItemSkus(tag: Tag) extends GenericTable.TableWithId[OrderLineItemSku](tag, "order_line_item_skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def skuShadowId = column[Int]("sku_shadow_id")

  def * = (id, skuId, skuShadowId) <> ((OrderLineItemSku.apply _).tupled, OrderLineItemSku.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object OrderLineItemSkus extends TableQueryWithId[OrderLineItemSku, OrderLineItemSkus](
  idLens = GenLens[OrderLineItemSku](_.id)
)(new OrderLineItemSkus(_)){

  def findBySkuId(id: Int): DBIO[Option[OrderLineItemSku]] =
    filter(_.skuId === id).one

  // we can safeGet here since we generate these records upon creation of the `skus` record via trigger
  def safeFindBySkuId(id: Int)(implicit ec: ExecutionContext): DBIO[OrderLineItemSku] =
    filter(_.skuId === id).one.safeGet

  def findByOrderId(orderId: Rep[Int]): QuerySeq = for {
    lis     ← OrderLineItems.filter(_.orderId === orderId)
    skuLis  ← lis.skuLineItems
  } yield skuLis

  type FindLineItemResult = (Sku, SkuShadow, OrderLineItem)

  def findLineItemsByOrder(order: Order): Query[ (Skus, SkuShadows, OrderLineItems), FindLineItemResult, Seq] = for {
    lis     ← OrderLineItems.filter(_.orderId === order.id)
    skuLis  ← lis.skuLineItems
    skuShadow ← SkuShadows if skuShadow.id === skuLis.skuShadowId
    sku     ← skuShadow.sku
  } yield (sku, skuShadow, lis)

  object scope {
    implicit class OrderLineItemSkusQuerySeqConversions(q: QuerySeq) {
      def withSkus: Query[(OrderLineItemSkus, Skus), (OrderLineItemSku, Sku), Seq] = for {
        items ← q
        skus  ← items.sku
      } yield (items, skus)
    }
  }
}
