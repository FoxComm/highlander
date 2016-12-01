package models.discount.offers

import models.account.User
import models.cord.lineitems.OrderLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import services.Authenticator.AuthData
import utils.aliases._

case class OrderAmountOffer(discount: Int) extends Offer with AmountOffer {

  val offerType: OfferType           = OrderAmountOff
  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(
      input: DiscountInput)(implicit db: DB, ec: EC, es: ES, auth: AuthData[User]): OfferResult =
    if (discount > 0)
      buildResult(input, subtract(input.cart.subTotal, discount))
    else
      pureResult()
}
