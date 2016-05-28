package models.discount.offers

import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case object FreeShippingOffer extends Offer {

  val offerType: OfferType           = FreeShipping
  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    input.shippingMethod match {
      case Some(sm) ⇒ accept(input, sm.price)
      case _        ⇒ reject()
    }
}
