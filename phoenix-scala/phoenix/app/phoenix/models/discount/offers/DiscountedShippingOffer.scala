package phoenix.models.discount.offers

import core.db.Result
import phoenix.models.discount.DiscountInput
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class DiscountedShippingOffer(discount: Long) extends Offer with AmountOffer {

  val offerType: OfferType = DiscountedShipping

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis): Result[Seq[OfferResult]] =
    input.shippingCost match {
      case Some(sc) if discount > 0 ⇒ buildResult(input, subtract(sc, discount))
      case _                        ⇒ pureResult()
    }
}
