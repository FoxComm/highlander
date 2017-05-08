package models.discount.offers

import models.cord.lineitems.CartLineItemAdjustment._
import models.discount._
import models.discount.offers.Offer.OfferResult
import utils.aliases._
import utils.apis.Apis

case class OrderPercentOffer(discount: Int) extends Offer with PercentOffer {

  val offerType: OfferType           = OrderPercentOff
  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    if (discount > 0 && discount < 100)
      buildResult(input, subtract(input.eligibleForDiscountSubtotal, discount))
    else
      pureResult()
}
