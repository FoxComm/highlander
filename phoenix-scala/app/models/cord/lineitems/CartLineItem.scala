package models.cord.lineitems

import models.cord.lineitems.OrderLineItems._
import models.inventory.{Sku, Skus}
import models.objects._
import models.product.Products
import utils.aliases.Json
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.aliases._
import utils.db._

case class CartLineItemProductData(sku: Sku,
                                   skuForm: ObjectForm,
                                   skuShadow: ObjectShadow,
                                   productForm: ObjectForm,
                                   productShadow: ObjectShadow,
                                   image: Option[String],
                                   lineItem: CartLineItem,
                                   attributes: Option[Json] = None)
    extends LineItemProductData[CartLineItem] {

  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = OrderLineItem.Cart
  def withLineItemReferenceNumber(newLineItemRef: String) =
    this.copy(lineItem = lineItem.copy(referenceNumber = newLineItemRef))
}

case class CartLineItem(id: Int = 0,
                        referenceNumber: String = "",
                        cordRef: String,
                        skuId: Int,
                        attributes: Option[Json] = None)
    extends FoxModel[CartLineItem]

class CartLineItems(tag: Tag) extends FoxTable[CartLineItem](tag, "cart_line_items") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def cordRef         = column[String]("cord_ref")
  def skuId           = column[Int]("sku_id")
  def attributes      = column[Option[Json]]("attributes")

  def * =
    (id, referenceNumber, cordRef, skuId, attributes) <> ((CartLineItem.apply _).tupled, CartLineItem.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object CartLineItems
    extends FoxTableQuery[CartLineItem, CartLineItems](new CartLineItems(_))
    with ReturningId[CartLineItem, CartLineItems] {

  val returningLens: Lens[CartLineItem, Int] = lens[CartLineItem].id

  def byCordRef(cordRef: String): QuerySeq = filter(_.cordRef === cordRef)

  def filterByProductId(productFormId: Int, contextId: Int): QuerySeq = {
    val productIds =
      Products.filter(p ⇒ p.formId === productFormId && p.contextId === contextId).map(_.id)
    val skuIds =
      ProductSkuLinks.join(productIds).on((link, productId) ⇒ link.leftId === productId).map {
        case (link, _) ⇒ link.rightId
      }

    join(skuIds).on((item, skuId) ⇒ item.skuId === skuId).map { case (item, _) ⇒ item }
  }

  def filterBySkuId(skuFormId: Int, contextId: Int): QuerySeq = {
    val skuIds = Skus.filter(_.formId === skuFormId).map(_.id)
    join(skuIds).on((item, skuId) ⇒ item.skuId === skuId).map { case (item, _) ⇒ item }
  }

  object scope {
    implicit class ExtractLineItems(q: QuerySeq) {
      // Map [SKU code → quantity in cart/order]
      def countSkus(implicit ec: EC): DBIO[Map[String, Int]] =
        (for {
          skuLineItems ← q
          sku          ← skuLineItems.sku
        } yield sku.code).result.map(_.foldLeft(Map[String, Int]()) {
          case (acc, skuCode) ⇒
            val quantity = acc.getOrElse(skuCode, 0)
            acc.updated(skuCode, quantity + 1)
        })
    }
  }
}
