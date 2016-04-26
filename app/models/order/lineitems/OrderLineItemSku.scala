package models.order.lineitems

import models.inventory.{Sku, Skus}
import models.order.Order
import models.product.{Product, Products}
import models.objects._
import utils.Money.Currency
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class OrderLineItemSku(id: Int = 0, skuId: Int, skuShadowId: Int)
  extends FoxModel[OrderLineItemSku]

case class OrderLineItemProductData(
  sku: Sku, 
  skuForm: ObjectForm, 
  skuShadow: ObjectShadow, 
  lineItem: OrderLineItem)

object OrderLineItemSku {}

class OrderLineItemSkus(tag: Tag) extends FoxTable[OrderLineItemSku](tag, "order_line_item_skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def skuShadowId = column[Int]("sku_shadow_id")

  def * = (id, skuId, skuShadowId) <> ((OrderLineItemSku.apply _).tupled, OrderLineItemSku.unapply)

  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, skuShadowId, ObjectShadows)(_.id)
}

object OrderLineItemSkus extends FoxTableQuery[OrderLineItemSku, OrderLineItemSkus](
  idLens = lens[OrderLineItemSku].id
)(new OrderLineItemSkus(_)){

  def findBySkuId(id: Int): DBIO[Option[OrderLineItemSku]] =
    filter(_.skuId === id).one

  // we can safeGet here since we generate these records upon creation of the `skus` record via trigger
  def safeFindBySkuId(id: Int)(implicit ec: EC): DBIO[OrderLineItemSku] =
    filter(_.skuId === id).one.safeGet

  def findByOrderId(orderId: Rep[Int]): QuerySeq = for {
    lis     ← OrderLineItems.filter(_.orderId === orderId)
    skuLis  ← lis.skuLineItems
  } yield skuLis

  type FindLineItemResult = (Sku, ObjectForm, ObjectShadow, OrderLineItem)

  def findLineItemsByOrder(order: Order): Query[ (Skus, ObjectForms, ObjectShadows, OrderLineItems), FindLineItemResult, Seq] = for {
    lis     ← OrderLineItems.filter(_.orderId === order.id)
    skuLis  ← lis.skuLineItems
    sku     ← skuLis.sku
    form ← ObjectForms if form.id === sku.formId
    shadow     ← skuLis.shadow
  } yield (sku, form, shadow, lis)

  object scope {
    implicit class OrderLineItemSkusQuerySeqConversions(q: QuerySeq) {
      def withSkus: Query[(OrderLineItemSkus, Skus), (OrderLineItemSku, Sku), Seq] = for {
        items ← q
        skus  ← items.sku
      } yield (items, skus)
    }
  }
}
