package models.discount.offers

import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._

case class OrderPercentOffer(discount: Int) extends Offer with PercentOffer {

  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput): OfferResult = {
    if (discount > 0 && discount < 100) {
      accept(input, substract(input.order.subTotal, discount))
    } else {
      reject(input, "Invalid discount value provided (should be between 1 and 99)")
    }
  }
}
