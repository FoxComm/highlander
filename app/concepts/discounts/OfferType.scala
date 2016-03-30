package concepts.discounts

import com.pellucid.sealerate
import utils.ADT

// Offer types
sealed trait OfferType
case object OrderPercentOff extends OfferType
case object OrderAmountOff extends OfferType
case object ItemsSinglePercentOff extends OfferType
case object ItemsSingleAmountOff extends OfferType
case object ItemsSelectPercentOff extends OfferType
case object ItemsSelectAmountOff extends OfferType
case object FreeShipping extends OfferType
case object DiscountedShipping extends OfferType

object OfferType extends ADT[OfferType] {
  def types = sealerate.values[OfferType]
}
