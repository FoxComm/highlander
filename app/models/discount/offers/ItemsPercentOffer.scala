package models.discount.offers

import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case class ItemsPercentOffer(discount: Int, search: ProductSearch)
    extends Offer
    with PercentOffer {

  val offerType: OfferType           = ItemsPercentOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (discount > 0 && discount < 100) adjustInner(input) else reject()

  // FIXME
  private def adjustInner(input: DiscountInput): OfferResult =
    accept(input, substract(totalByProduct(input.lineItems, search.productSearchId), discount))
}
