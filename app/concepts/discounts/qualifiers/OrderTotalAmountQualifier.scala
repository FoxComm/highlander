package concepts.discounts.qualifiers

import concepts.discounts._

final case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val promoType: PromoType = OrderPromo
  val qualifierType: QualifierType = OrderTotalAmount
}
