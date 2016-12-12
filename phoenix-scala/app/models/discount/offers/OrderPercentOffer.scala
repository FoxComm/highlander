package models.discount.offers

import models.cord.lineitems.OrderLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import utils.aliases._

case class OrderPercentOffer(discount: Int) extends Offer with PercentOffer {

  val offerType: OfferType           = OrderPercentOff
  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult =
    if (discount > 0 && discount < 100)
      buildResult(input, subtract(input.cart.subTotal, discount))
    else
      pureResult()
}
