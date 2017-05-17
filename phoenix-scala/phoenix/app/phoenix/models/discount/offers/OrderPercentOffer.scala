package phoenix.models.discount.offers

import phoenix.models.cord.lineitems.CartLineItemAdjustment._
import phoenix.models.discount._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class OrderPercentOffer(discount: Int) extends Offer with PercentOffer {

  val offerType: OfferType           = OrderPercentOff
  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    if (discount > 0 && discount < 100)
      buildResult(input, subtract(input.eligibleForDiscountSubtotal, discount))
    else
      pureResult()
}
