package models.discount.offers

import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case class OrderPercentOffer(discount: Int) extends Offer with PercentOffer {

  val offerType: OfferType           = OrderPercentOff
  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (discount > 0 && discount < 100) accept(input, substract(input.order.subTotal, discount))
    else reject()
}
