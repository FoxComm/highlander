package models.cord.lineitems

import cats.implicits._
import models.inventory.{ProductVariant, ProductVariants}
import models.objects._
import org.json4s.Extraction.decompose
import org.json4s.Formats
import shapeless._
import utils.JsonFormatters
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CartLineItemProductData(productVariant: ProductVariant,
                                   productVariantForm: ObjectForm,
                                   productVariantShadow: ObjectShadow,
                                   productForm: ObjectForm,
                                   productShadow: ObjectShadow,
                                   image: Option[String],
                                   lineItem: CartLineItem,
                                   attributes: Option[LineItemAttributes] = None)
    extends LineItemProductData[CartLineItem] {

  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = OrderLineItem.Cart
  def withLineItemReferenceNumber(newLineItemRef: String) =
    this.copy(lineItem = lineItem.copy(referenceNumber = newLineItemRef))
}

case class CartLineItem(id: Int = 0,
                        referenceNumber: String = "",
                        cordRef: String,
                        productVariantId: Int,
                        attributes: Option[LineItemAttributes] = None)
    extends FoxModel[CartLineItem]

class CartLineItems(tag: Tag) extends FoxTable[CartLineItem](tag, "cart_line_items") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber  = column[String]("reference_number")
  def cordRef          = column[String]("cord_ref")
  def productVariantId = column[Int]("product_variant_id")
  def attributes       = column[Option[Json]]("attributes")

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def * =
    (id, referenceNumber, cordRef, productVariantId, attributes).shaped <>
      ({
        case (id, refNum, cordRef, variantId, attrs) ⇒
          CartLineItem(id,
                       refNum,
                       cordRef,
                       variantId,
                       attrs.flatMap(_.extractOpt[LineItemAttributes]))
      }, { cli: CartLineItem ⇒
        (cli.id,
         cli.referenceNumber,
         cli.cordRef,
         cli.productVariantId,
         cli.attributes.map(decompose)).some
      })

  def productVariant =
    foreignKey(ProductVariants.tableName, productVariantId, ProductVariants)(_.id)
}

object CartLineItems
    extends FoxTableQuery[CartLineItem, CartLineItems](new CartLineItems(_))
    with ReturningId[CartLineItem, CartLineItems] {

  val returningLens: Lens[CartLineItem, Int] = lens[CartLineItem].id

  def byCordRef(cordRef: String): QuerySeq = filter(_.cordRef === cordRef)

  object scope {
    implicit class ExtractLineItems(q: QuerySeq) {
      // Map [product variant ID → quantity in cart/order]
      def countProductVariants(implicit ec: EC): DBIO[Map[ObjectForm#Id, Int]] =
        (for {
          cartLineItems  ← q
          productVariant ← cartLineItems.productVariant
        } yield productVariant.formId).result.map(_.groupBy(identity).mapValues(_.size))
    }
  }
}
