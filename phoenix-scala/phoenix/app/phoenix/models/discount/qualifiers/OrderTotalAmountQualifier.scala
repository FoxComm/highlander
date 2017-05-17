package phoenix.models.discount.qualifiers

import phoenix.models.discount._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import utils.db._

case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderTotalAmount

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    if (input.eligibleForDiscountSubtotal >= totalAmount) accept()
    else reject(input, s"Order subtotal is less than $totalAmount")
}
