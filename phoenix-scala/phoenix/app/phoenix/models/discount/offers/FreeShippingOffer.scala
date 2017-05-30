package phoenix.models.discount.offers

import core.db.Result
import phoenix.models.discount.DiscountInput
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case object FreeShippingOffer extends Offer {

  val offerType: OfferType = FreeShipping

  def adjust(input: DiscountInput)(implicit db: DB,
                                   ec: EC,
                                   apis: Apis,
                                   au: AU): Result[Seq[OfferResult]] =
    input.shippingMethod match {
      case Some(sm) ⇒ buildResult(input, sm.price)
      case _        ⇒ pureResult()
    }
}
