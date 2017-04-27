package models.discount.qualifiers

import models.discount.DiscountInput
import utils.aliases._
import utils.apis.Apis
import utils.db._

case object OrderAnyQualifier extends Qualifier {

  val qualifierType: QualifierType = OrderAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    if (input.isEligibleForDiscount) accept()
    else reject(input, "Items in cart are not eligible for discount")
}
