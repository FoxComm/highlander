package models.discount.offers

import models.account.User
import models.cord.lineitems.OrderLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import services.Authenticator.AuthData
import utils.aliases._

case class DiscountedShippingOffer(discount: Int) extends Offer with AmountOffer {

  val offerType: OfferType           = DiscountedShipping
  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(
      input: DiscountInput)(implicit db: DB, ec: EC, es: ES, auth: AuthData[User]): OfferResult =
    input.shippingMethod match {
      case Some(sm) if discount > 0 ⇒ buildResult(input, subtract(sm.price, discount))
      case _                        ⇒ pureResult()
    }
}
