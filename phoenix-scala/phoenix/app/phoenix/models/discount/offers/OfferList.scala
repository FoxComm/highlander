package phoenix.models.discount.offers

import cats.implicits._
import phoenix.models.cord.lineitems.CartLineItemAdjustment._
import phoenix.models.discount.DiscountInput
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import utils.db._

case class OfferList(offers: Seq[Offer]) extends Offer {

  val offerType: OfferType           = ListCombinator
  val adjustmentType: AdjustmentType = Combinator

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    Result.seqCollectFailures(offers.map(_.adjust(input)).toList).map(_.flatten)

}
