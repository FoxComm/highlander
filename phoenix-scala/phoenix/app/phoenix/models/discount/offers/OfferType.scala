package phoenix.models.discount.offers

import com.pellucid.sealerate
import phoenix.utils.ADT

// Offer types
sealed trait OfferType
case object OrderPercentOff    extends OfferType
case object OrderAmountOff     extends OfferType
case object ItemPercentOff     extends OfferType
case object ItemAmountOff      extends OfferType
case object ItemsPercentOff    extends OfferType
case object ItemsAmountOff     extends OfferType
case object FreeShipping       extends OfferType
case object DiscountedShipping extends OfferType
case object SetPrice           extends OfferType
case object ListCombinator     extends OfferType

object OfferType extends ADT[OfferType] {
  def types = sealerate.values[OfferType]
}
