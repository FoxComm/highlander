package models.discount.qualifiers

import models.discount._
import services.Result
import utils.aliases._

case object OrderAnyQualifier extends Qualifier {

  val qualifierType: QualifierType = OrderAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    if (input.isEligibleForDiscount) accept()
    else reject(input, "Items in cart are not eligible for discount")
}
