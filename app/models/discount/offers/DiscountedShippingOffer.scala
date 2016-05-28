package models.discount.offers

import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._

case class DiscountedShippingOffer(discount: Int) extends Offer with AmountOffer {

  val offerType: OfferType           = DiscountedShipping
  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput): OfferResult = input.shippingMethod match {
    case Some(sm) if discount > 0 ⇒ accept(input, substract(sm.price, discount))
    case _                        ⇒ reject()
  }
}
