package models.discount.offers

import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment.{AdjustmentType, LineItemAdjustment}
import utils.aliases._

case class ItemsAmountOffer(discount: Int, search: SearchReference)
    extends Offer
    with AmountOffer {

  val offerType: OfferType           = ItemsAmountOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (discount > 0) adjustInner(input) else reject()

  private def adjustInner(input: DiscountInput): OfferResult = search match {
    case ProductSearch(formId) ⇒
      accept(input, substract(totalByProduct(input.lineItems, formId), discount))
    //case SkuSearch(code) ⇒ accept(input, substract(totalBySku(input.lineItems, code), discount))
    case _ ⇒ reject()
  }
}
