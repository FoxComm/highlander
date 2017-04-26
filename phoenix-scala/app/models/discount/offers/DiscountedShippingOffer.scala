package models.discount.offers

import io.circe.syntax._
import models.cord.lineitems.CartLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import utils.aliases._
import utils.apis.Apis
import utils.json.codecs._

case class DiscountedShippingOffer(discount: Int) extends Offer with AmountOffer {

  val offerType: OfferType           = DiscountedShipping
  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    input.shippingMethod match {
      case Some(sm) if discount > 0 ⇒ buildResult(input, subtract(sm.price, discount))
      case _                        ⇒ pureResult()
    }

  def json: Json = this.asJson
}
