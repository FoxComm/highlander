package models.discount.offers

import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case class SetPriceOffer(setPrice: Int, numUnits: Int, search: SearchReference)
    extends Offer
    with SetOffer {

  val offerType: OfferType           = SetPrice
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (setPrice > 0 && numUnits > 0) adjustInner(input) else reject()

  private def adjustInner(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    search match {
      case ProductSearch(formId) ⇒
        val takeItems = input.lineItems.filter(_.product.formId == formId).take(numUnits)
        accept(input, substract(totalByProduct(takeItems, formId), setPrice))
      /*
    case SkuSearch(code) ⇒
      val takeItems = input.lineItems.filter(_.sku.code == code).take(numUnits)
      accept(input, substract(totalBySku(takeItems, code), setPrice))
       */
      case _ ⇒
        reject()
    }
}
