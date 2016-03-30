package concepts.discounts.qualifiers

import concepts.discounts._

case object ItemsAnyQualifier extends Qualifier {

  val promoType: PromoType = ItemsPromo
  val qualifierType: QualifierType = ItemsAny
}
