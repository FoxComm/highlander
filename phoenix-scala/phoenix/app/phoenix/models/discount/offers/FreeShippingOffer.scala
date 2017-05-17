package phoenix.models.discount.offers

import phoenix.models.cord.lineitems.CartLineItemAdjustment._
import phoenix.models.discount.DiscountInput
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case object FreeShippingOffer extends Offer {

  val offerType: OfferType           = FreeShipping
  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    input.shippingMethod match {
      case Some(sm) ⇒ buildResult(input, sm.price)
      case _        ⇒ pureResult()
    }
}
