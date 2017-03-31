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
import utils.apis.Apis

// Percent off all matched items in cart
case class ItemsPercentOffer(discount: Int, search: Seq[ProductSearch])
    extends Offer
    with PercentOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType           = ItemsPercentOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    if (discount > 0 && discount < 100) adjustInner(input)(search) else pureResult()

  def matchXor(input: DiscountInput)(
      xor: Failures Xor Buckets): Failures Xor Seq[OrderLineItemAdjustment] =
    xor match {
      case Xor.Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        val adjustments = input.lineItems
          .filter(data ⇒ matchedFormIds.contains(data.productForm.id.toString))
          .map { data ⇒
            build(input, subtract(price(data), discount), data.lineItemReferenceNumber.some)
          }

        Xor.Right(adjustments)
      case _ ⇒ pureXor()
    }
}
