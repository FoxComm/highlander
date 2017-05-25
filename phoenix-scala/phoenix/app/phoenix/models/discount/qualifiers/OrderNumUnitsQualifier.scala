package phoenix.models.discount.qualifiers

import core.db._
import phoenix.models.discount._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class OrderNumUnitsQualifier(numUnits: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderNumUnits

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    if (input.eligibleForDiscountNumItems >= numUnits) accept()
    else reject(input, s"Order unit count is less than $numUnits")
}
