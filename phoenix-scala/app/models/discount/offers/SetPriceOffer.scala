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

case class SetPriceOffer(setPrice: Int, numUnits: Int, search: Seq[ProductSearch])
    extends Offer
    with SetOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType           = SetPrice
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult =
    if (setPrice > 0 && numUnits < 100) adjustInner(input)(search) else pureResult()

  def matchXor(input: DiscountInput)(
      xor: Failures Xor Buckets): Failures Xor Seq[OrderLineItemAdjustment] =
    xor match {
      case Xor.Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        val adjustments = input.lineItems
          .filter(data ⇒ matchedFormIds.contains(data.productForm.id.toString))
          .take(numUnits)
          .map { data ⇒
            build(input, subtract(price(data), setPrice), data.lineItemReferenceNumber.some)
          }

        Xor.Right(adjustments)
      case _ ⇒ pureXor()
    }
}
