package models.cord.lineitems
import models.inventory._
import models.objects._
import models.product._
import utils.aliases._
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CartLineItemProductData(sku: Sku,
                                   skuForm: ObjectForm,
                                   skuShadow: ObjectShadow,
                                   product: ObjectShadow,
                                   lineItem: CartLineItem)
    extends LineItemProductData[CartLineItem] {

  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = OrderLineItem.Cart
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

  type FindLineItemResult      = (Sku, ObjectForm, ObjectShadow, ObjectShadow, CartLineItem)
  type FindLineItemResultMulti = (Skus, ObjectForms, ObjectShadows, ObjectShadows, CartLineItems)

  object scope {
    implicit class ExtractLineItems(q: QuerySeq) {

      def lineItems: Query[FindLineItemResultMulti, FindLineItemResult, Seq] =
        for {
          skuLineItems  ← q
          sku           ← skuLineItems.sku
          skuForm       ← ObjectForms if skuForm.id === sku.formId
          skuShadow     ← sku.shadow
          link          ← ProductSkuLinks if link.rightId === sku.id
          product       ← Products if product.id === link.leftId
          productShadow ← ObjectShadows if productShadow.id === product.shadowId
        } yield (sku, skuForm, skuShadow, productShadow, skuLineItems)

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
