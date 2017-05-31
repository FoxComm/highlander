package phoenix.models.discount.offers

import cats.implicits._
import core.db._
import phoenix.models.discount.DiscountInput
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class OfferList(offers: Seq[Offer]) extends Offer {

  val offerType: OfferType = ListCombinator

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis): Result[Seq[OfferResult]] =
    Result.seqCollectFailures(offers.map(_.adjust(input)).toList).map(_.flatten)

}
