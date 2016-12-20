package models.discount.offers

import cats.data.Xor
import cats.implicits._
import failures._
import models.cord.lineitems.OrderLineItemAdjustment
import models.cord.lineitems.OrderLineItemAdjustment._
import models.discount._
import models.discount.offers.Offer.OfferResult
import utils.ElasticsearchApi._
import utils.aliases._

// Percent off single item
case class ItemPercentOffer(discount: Int, search: Seq[ProductSearch])
    extends Offer
    with PercentOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType           = ItemPercentOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult =
    if (discount > 0 && discount < 100) adjustInner(input)(search) else pureResult()

  def matchXor(input: DiscountInput)(
      xor: Failures Xor Buckets): Failures Xor Seq[OrderLineItemAdjustment] =
    xor match {
      case Xor.Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        input.lineItems.find(data ⇒ matchedFormIds.contains(data.productForm.id.toString)) match {
          case Some(data) ⇒
            buildXor(input, subtract(price(data), discount), data.lineItemReferenceNumber.some)
          case _ ⇒ pureXor()
        }
      case _ ⇒ pureXor()
    }
}
