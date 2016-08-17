package models.cord.lineitems

import models.inventory.{Sku, Skus}
import models.objects._
import models.product.Products
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class OrderLineItemSku(id: Int = 0, skuId: Int, skuShadowId: Int)
    extends FoxModel[OrderLineItemSku]

trait LineItemProductData[LI] {
  def sku: Sku
  def skuForm: ObjectForm
  def skuShadow: ObjectShadow
  def product: ObjectShadow
  def lineItem: LI

  def lineItemReferenceNumber: String
  def lineItemState: OrderLineItem.State
}

case class OrderLineItemProductData(sku: Sku,
                                    skuForm: ObjectForm,
                                    skuShadow: ObjectShadow,
                                    product: ObjectShadow,
                                    lineItem: OrderLineItem)
    extends LineItemProductData[OrderLineItem] {
  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = lineItem.state
}

case class CartLineItemProductData(sku: Sku,
                                   skuForm: ObjectForm,
                                   skuShadow: ObjectShadow,
                                   product: ObjectShadow,
                                   lineItem: CartLineItemSku)
    extends LineItemProductData[CartLineItemSku] {

  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = OrderLineItem.Cart
}

object OrderLineItemSku {}

class OrderLineItemSkus(tag: Tag) extends FoxTable[OrderLineItemSku](tag, "order_line_item_skus") {
  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId       = column[Int]("sku_id")
  def skuShadowId = column[Int]("sku_shadow_id")

  def * = (id, skuId, skuShadowId) <> ((OrderLineItemSku.apply _).tupled, OrderLineItemSku.unapply)

  def sku    = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, skuShadowId, ObjectShadows)(_.id)
}

object OrderLineItemSkus
    extends FoxTableQuery[OrderLineItemSku, OrderLineItemSkus](new OrderLineItemSkus(_))
    with ReturningId[OrderLineItemSku, OrderLineItemSkus] {

  val returningLens: Lens[OrderLineItemSku, Int] = lens[OrderLineItemSku].id

  def findBySkuId(id: Int): DBIO[Option[OrderLineItemSku]] =
    filter(_.skuId === id).one

  type FindLineItemResult      = (Sku, ObjectForm, ObjectShadow, ObjectShadow, OrderLineItem)
  type FindLineItemResultMulti = (Skus, ObjectForms, ObjectShadows, ObjectShadows, OrderLineItems)

  def findLineItemsByCordRef(
      refNum: String): Query[FindLineItemResultMulti, FindLineItemResult, Seq] =
    for {
      lineItems     ← OrderLineItems.filter(_.cordRef === refNum)
      skuLineItems  ← lineItems.skuLineItems
      sku           ← skuLineItems.sku
      skuForm       ← ObjectForms if skuForm.id === sku.formId
      skuShadow     ← skuLineItems.shadow
      link          ← ProductSkuLinks if link.rightId === sku.id
      product       ← Products if product.id === link.rightId
      productShadow ← ObjectShadows if productShadow.id === product.shadowId
    } yield (sku, skuForm, skuShadow, productShadow, lineItems)

  object scope {
    implicit class OrderLineItemSkusQuerySeqConversions(q: QuerySeq) {
      def withSkus: Query[(OrderLineItemSkus, Skus), (OrderLineItemSku, Sku), Seq] =
        for {
          items ← q
          skus  ← items.sku
        } yield (items, skus)
    }
  }
}
