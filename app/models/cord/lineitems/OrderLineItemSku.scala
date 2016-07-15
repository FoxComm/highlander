package models.cord.lineitems

import models.inventory.{Sku, Skus}
import models.objects.ObjectLink._
import models.objects._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class OrderLineItemSku(id: Int = 0, skuId: Int, skuShadowId: Int)
    extends FoxModel[OrderLineItemSku]

case class OrderLineItemProductData(sku: Sku,
                                    skuForm: ObjectForm,
                                    skuShadow: ObjectShadow,
                                    product: ObjectShadow,
                                    lineItem: OrderLineItem)

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

  // we can safeGet here since we generate these records upon creation of the `skus` record via trigger
  def safeFindBySkuId(id: Int)(implicit ec: EC): DBIO[OrderLineItemSku] =
    filter(_.skuId === id).one.safeGet

  def findByOrderRef(cordRef: Rep[String]): QuerySeq =
    for {
      lis    ← OrderLineItems.filter(_.cordRef === cordRef)
      skuLis ← lis.skuLineItems
    } yield skuLis

  type FindLineItemResult      = (Sku, ObjectForm, ObjectShadow, ObjectShadow, OrderLineItem)
  type FindLineItemResultMulti = (Skus, ObjectForms, ObjectShadows, ObjectShadows, OrderLineItems)

  def findLineItemsByCordRef(
      refNum: String): Query[FindLineItemResultMulti, FindLineItemResult, Seq] =
    for {
      lineItems    ← OrderLineItems.filter(_.cordRef === refNum)
      skuLineItems ← lineItems.skuLineItems
      sku          ← skuLineItems.sku
      skuForm      ← ObjectForms if skuForm.id === sku.formId
      skuShadow    ← skuLineItems.shadow
      link         ← ObjectLinks if link.rightId === skuShadow.id &&
        link.linkType === (ProductSku: LinkType)
      productShadow ← ObjectShadows if productShadow.id === link.rightId
    } yield (sku, skuForm, skuShadow, productShadow, lineItems)

  // Map [SKU code → quantity in cart/order]
  def countSkusByCordRef(refNum: String)(implicit ec: EC): DBIO[Map[String, Int]] =
    OrderLineItemSkus.findLineItemsByCordRef(refNum).result.map { li ⇒
      li.foldLeft(Map[String, Int]()) {
        case (acc, (sku, _, _, _, _)) ⇒
          val quantity = acc.getOrElse(sku.code, 0)
          acc.updated(sku.code, quantity + 1)
      }
    }

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
