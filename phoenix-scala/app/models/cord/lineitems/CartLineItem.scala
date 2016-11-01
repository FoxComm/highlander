package models.cord.lineitems

import models.cord.lineitems.OrderLineItems._
import models.inventory.{Skus, Sku}
import models.objects.{ProductSkuLinks, ObjectShadows, ObjectForms, ObjectShadow, ObjectForm}
import models.product.Products
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.aliases.EC
import utils.db.{FoxModel, ReturningId, FoxTableQuery, FoxTable}

case class CartLineItemProductData(sku: Sku,
                                   skuForm: ObjectForm,
                                   skuShadow: ObjectShadow,
                                   productForm: ObjectForm,
                                   productShadow: ObjectShadow,
                                   image: Option[String],
                                   lineItem: CartLineItem)
    extends LineItemProductData[CartLineItem] {

  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = OrderLineItem.Cart
  def withLineItemReferenceNumber(newLineItemRef: String) =
    this.copy(lineItem = lineItem.copy(referenceNumber = newLineItemRef))
}

case class CartLineItem(id: Int = 0, referenceNumber: String = "", cordRef: String, skuId: Int)
    extends FoxModel[CartLineItem]

class CartLineItems(tag: Tag) extends FoxTable[CartLineItem](tag, "cart_line_items") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def cordRef         = column[String]("cord_ref")
  def skuId           = column[Int]("sku_id")

  def * =
    (id, referenceNumber, cordRef, skuId) <> ((CartLineItem.apply _).tupled, CartLineItem.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object CartLineItems
    extends FoxTableQuery[CartLineItem, CartLineItems](new CartLineItems(_))
    with ReturningId[CartLineItem, CartLineItems] {

  val returningLens: Lens[CartLineItem, Int] = lens[CartLineItem].id

  def byCordRef(cordRef: String): QuerySeq = filter(_.cordRef === cordRef)

  object scope {
    implicit class ExtractLineItems(q: QuerySeq) {
      // Map [SKU ID → quantity in cart/order]
      def countSkus(implicit ec: EC): DBIO[Map[Int, Int]] =
        (for {
          skuLineItems ← q
          sku          ← skuLineItems.sku
        } yield sku.formId).result.map(_.foldLeft(Map[Int, Int]()) {
          case (acc, skuId) ⇒
            val quantity = acc.getOrElse(skuId, 0)
            acc.updated(skuId, quantity + 1)
        })
    }
  }
}
