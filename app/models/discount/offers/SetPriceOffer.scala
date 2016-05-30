package models.discount.offers

import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case class SetPriceOffer(setPrice: Int, numUnits: Int, search: ProductSearch)
    extends Offer
    with SetOffer {

  val offerType: OfferType           = SetPrice
  val adjustmentType: AdjustmentType = LineItemAdjustment

  // FIXME
  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (setPrice > 0 && numUnits > 0) {
      val takeItems =
        input.lineItems.filter(_.product.formId == search.productSearchId).take(numUnits)
      accept(input, substract(totalByProduct(takeItems, search.productSearchId), setPrice))
    } else
      reject()
}
