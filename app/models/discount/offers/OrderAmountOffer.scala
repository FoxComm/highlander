package models.discount.offers

import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case class OrderAmountOffer(discount: Int) extends Offer with AmountOffer {

  val offerType: OfferType           = OrderAmountOff
  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (discount > 0)
      buildResult(input, substract(input.order.subTotal, discount))
    else
      pure()
}
