package concepts.discounts

import com.pellucid.sealerate
import utils.ADT

// Promotion types
sealed trait PromoType
case object OrderPromo extends PromoType
case object ItemsPromo extends PromoType

object PromoType extends ADT[PromoType] {
  def types = sealerate.values[PromoType]
}

// Linking mechanism for qualifiers / offers
sealed trait ReferenceType
case object Product extends ReferenceType
case object Sku extends ReferenceType
case object SharedSearch extends ReferenceType

object ReferenceType extends ADT[ReferenceType] {
  def types = sealerate.values[ReferenceType]
}

// Qualifier types
sealed trait QualifierType
case object OrderAny extends QualifierType
case object OrderTotalAmount extends QualifierType
case object OrderNumUnits extends QualifierType
case object ItemsAny extends QualifierType
case object ItemsTotalAmount extends QualifierType
case object ItemsNumUnits extends QualifierType

object QualifierType extends ADT[QualifierType] {
  def types = sealerate.values[QualifierType]
}

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