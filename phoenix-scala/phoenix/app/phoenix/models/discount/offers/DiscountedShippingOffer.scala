package phoenix.models.discount.offers

import phoenix.models.cord.lineitems.CartLineItemAdjustment._
import phoenix.models.discount.DiscountInput
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class DiscountedShippingOffer(discount: Int) extends Offer with AmountOffer {

  val offerType: OfferType           = DiscountedShipping
  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    input.shippingMethod match {
      case Some(sm) if discount > 0 ⇒ buildResult(input, subtract(sm.price, discount))
      case _                        ⇒ pureResult()
    }
}
