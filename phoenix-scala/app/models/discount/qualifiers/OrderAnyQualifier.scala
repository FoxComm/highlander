package models.discount.qualifiers

import models.discount.DiscountInput
import services.Result
import utils.aliases._

case object OrderAnyQualifier extends Qualifier {

  val qualifierType: QualifierType = OrderAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    Result.unit
}
