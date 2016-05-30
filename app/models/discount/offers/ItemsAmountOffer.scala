package models.discount.offers

import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment.{AdjustmentType, LineItemAdjustment}
import utils.aliases._

case class ItemsAmountOffer(discount: Int, search: ProductSearch) extends Offer with AmountOffer {

  val offerType: OfferType           = ItemsAmountOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (discount > 0) adjustInner(input) else reject()

  // FIXME
  private def adjustInner(input: DiscountInput): OfferResult =
    accept(input, substract(totalByProduct(input.lineItems, 0), discount))
}
