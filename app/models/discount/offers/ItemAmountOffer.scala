package models.discount.offers

import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._
import utils.aliases._

case class ItemAmountOffer(discount: Int, search: SearchReference) extends Offer with AmountOffer {

  val offerType: OfferType           = ItemAmountOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (discount > 0) adjustInner(input) else reject()

  private def adjustInner(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    search match {
      case ProductSearch(formId) ⇒
        input.lineItems.find(_.product.formId == formId) match {
          case Some(data) ⇒ accept(input, substract(price(data), discount))
          case _          ⇒ reject()
        }
      /*
    case SkuSearch(code) ⇒
      input.lineItems.find(_.sku.code == code) match {
        case Some(data) ⇒ accept(input, substract(price(data), discount))
        case _          ⇒ reject()
      }
       */
      case _ ⇒
        reject()
    }
}
