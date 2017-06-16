package phoenix.models.discount.qualifiers

import core.db._
import phoenix.models.discount.DiscountInput
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case object OrderAnyQualifier extends Qualifier {

  val qualifierType: QualifierType = OrderAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    if (input.isEligibleForDiscount) accept()
    else reject(input, "Items in cart are not eligible for discount")
}
