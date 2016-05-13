package models.discount.offers

import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._

case object FreeShippingOffer extends Offer {

  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput): OfferResult = input.shippingMethod match {
    case Some(sm) ⇒ accept(input, sm.price)
    case _        ⇒ reject(input, "Order has no shipping method")
  }
}
