package phoenix.models.cord.lineitems

import cats.implicits._
import core.db.ExPostgresDriver.api._
import core.db._
import objectframework.models._
import org.json4s.Extraction.decompose
import org.json4s.Formats
import phoenix.models.inventory.{Sku, Skus}
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import shapeless._

case class CartLineItemProductData(sku: Sku,
                                   skuForm: ObjectForm,
                                   skuShadow: ObjectShadow,
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
                        skuId: Int,
                        attributes: Option[LineItemAttributes] = None)
    extends FoxModel[CartLineItem]

class CartLineItems(tag: Tag) extends FoxTable[CartLineItem](tag, "cart_line_items") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def cordRef         = column[String]("cord_ref")
  def skuId           = column[Int]("sku_id")
  def attributes      = column[Option[Json]]("attributes")

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def * =
    (id, referenceNumber, cordRef, skuId, attributes).shaped <>
      ({
        case (id, refNum, cordRef, skuId, attrs) ⇒
          CartLineItem(id, refNum, cordRef, skuId, attrs.flatMap(_.extractOpt[LineItemAttributes]))
      }, { cli: CartLineItem ⇒
        (cli.id, cli.referenceNumber, cli.cordRef, cli.skuId, cli.attributes.map(decompose)).some
      })

  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object CartLineItems
    extends FoxTableQuery[CartLineItem, CartLineItems](new CartLineItems(_))
    with ReturningId[CartLineItem, CartLineItems] {

  val returningLens: Lens[CartLineItem, Int] = lens[CartLineItem].id

  def byCordRef(cordRef: String): QuerySeq = filter(_.cordRef === cordRef)

  object scope {
    implicit class ExtractLineItems(q: QuerySeq) {
      // Map [SKU code → quantity in cart/order]
      def countSkus(implicit ec: EC): DBIO[Map[String, Int]] =
        (for {
          skuLineItems ← q
          sku          ← skuLineItems.sku
        } yield sku.code).result.map(_.groupBy(identity).mapValues(_.size))
    }
  }
}
