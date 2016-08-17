package models.cord.lineitems

import models.cord.lineitems.OrderLineItems._
import models.inventory.{Skus, Sku}
import models.objects.{ProductSkuLinks, ObjectShadows, ObjectForms, ObjectShadow, ObjectForm}
import models.product.Products
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.aliases.EC
import utils.db.{FoxModel, ReturningId, FoxTableQuery, FoxTable}

case class CartLineItemSku(id: Int = 0, referenceNumber: String = "", cordRef: String, skuId: Int)
    extends FoxModel[CartLineItemSku]

class CartLineItemSkus(tag: Tag) extends FoxTable[CartLineItemSku](tag, "cart_line_item_skus") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def cordRef         = column[String]("cord_ref")
  def skuId           = column[Int]("sku_id")

  def * =
    (id, referenceNumber, cordRef, skuId) <> ((CartLineItemSku.apply _).tupled, CartLineItemSku.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object CartLineItemSkus
    extends FoxTableQuery[CartLineItemSku, CartLineItemSkus](new CartLineItemSkus(_))
    with ReturningId[CartLineItemSku, CartLineItemSkus] {

  val returningLens: Lens[CartLineItemSku, Int] = lens[CartLineItemSku].id

  def byCordRef(cordRef: String): QuerySeq = filter(_.cordRef === cordRef)

  type FindLineItemResult = (Sku, ObjectForm, ObjectShadow, ObjectShadow, CartLineItemSku)
  type FindLineItemResultMulti =
    (Skus, ObjectForms, ObjectShadows, ObjectShadows, CartLineItemSkus)

  object scope {
    implicit class ExtractLineItems(q: QuerySeq) {

      def lineItems: Query[FindLineItemResultMulti, FindLineItemResult, Seq] =
        for {
          skuLineItems  ← q
          sku           ← skuLineItems.sku
          skuForm       ← ObjectForms if skuForm.id === sku.formId
          skuShadow     ← sku.shadow
          link          ← ProductSkuLinks if link.rightId === sku.id
          product       ← Products if product.id === link.rightId
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
