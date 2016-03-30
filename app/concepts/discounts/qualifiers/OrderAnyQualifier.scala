package concepts.discounts.qualifiers

import concepts.discounts._

case object OrderAnyQualifier extends Qualifier {

  val promoType: PromoType = OrderPromo
  val qualifierType: QualifierType = OrderAny
}
