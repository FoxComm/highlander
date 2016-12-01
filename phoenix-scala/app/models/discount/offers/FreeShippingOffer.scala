package models.discount.offers

import models.account.User
import models.cord.lineitems.OrderLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import services.Authenticator.AuthData
import utils.aliases._

case object FreeShippingOffer extends Offer {

  val offerType: OfferType           = FreeShipping
  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(
      input: DiscountInput)(implicit db: DB, ec: EC, es: ES, auth: AuthData[User]): OfferResult =
    input.shippingMethod match {
      case Some(sm) ⇒ buildResult(input, sm.price)
      case _        ⇒ pureResult()
    }
}
