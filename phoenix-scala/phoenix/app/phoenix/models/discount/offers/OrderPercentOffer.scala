package phoenix.models.discount.offers

import core.db.Result
import phoenix.models.discount._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class OrderPercentOffer(discount: Long) extends Offer with PercentOffer {

  val offerType: OfferType = OrderPercentOff

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis): Result[Seq[OfferResult]] =
    if (discount > 0 && discount < 100)
      buildResult(input, subtract(input.eligibleForDiscountSubtotal, discount))
    else
      pureResult()
}
