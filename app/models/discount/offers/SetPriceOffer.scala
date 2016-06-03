package models.discount.offers

import cats.implicits._
import cats.data.Xor
import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case class SetPriceOffer(setPrice: Int, numUnits: Int, search: ProductSearch)
    extends Offer
    with SetOffer {

  val offerType: OfferType           = SetPrice
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (setPrice > 0 && numUnits < 100) adjustInner(input) else pure()

  private def adjustInner(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    search.query(input).map {
      case Xor.Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)

        val adjustments = input.lineItems
          .filter(data ⇒ matchedFormIds.contains(data.product.formId.toString))
          .take(numUnits)
          .map { data ⇒
            build(input, substract(price(data), setPrice), data.lineItem.referenceNumber.some)
          }

        Xor.Right(adjustments)
      case _ ⇒ pureXor()
    }
}
