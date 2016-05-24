package models.discount.offers

import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._

case class ItemAmountOffer(discount: Int, search: SearchReference) extends Offer with AmountOffer {

  val offerType: OfferType           = ItemAmountOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput): OfferResult = {
    if (discount > 0) adjustInner(input) else reject(input, "Invalid discount value")
  }

  private def adjustInner(input: DiscountInput): OfferResult = search match {
    case ProductSearch(formId) ⇒
      input.lineItems.find(_.product.formId == formId) match {
        case Some(data) ⇒ accept(input, substract(price(data), discount))
        case _          ⇒ reject(input, "Product magically dissapeared from the order")
      }
    case SkuSearch(code) ⇒
      input.lineItems.find(_.sku.code == code) match {
        case Some(data) ⇒ accept(input, substract(price(data), discount))
        case _          ⇒ reject(input, "Sku magically dissapeared from the order")
      }
    case _ ⇒
      reject(input, "Invalid search type")
  }
}
