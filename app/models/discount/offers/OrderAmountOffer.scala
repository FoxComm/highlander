package models.discount.offers

import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._

case class OrderAmountOffer(discount: Int) extends Offer with AmountOffer {

  val offerType: OfferType           = OrderAmountOff
  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput): OfferResult = {
    if (discount > 0) {
      accept(input, substract(input.order.subTotal, discount))
    } else {
      reject(input, "Invalid discount value provided (should be bigger than zero)")
    }
  }
}
