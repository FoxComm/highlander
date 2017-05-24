package models.discount.qualifiers

import models.discount._
import utils.aliases._
import utils.apis.Apis
import utils.db._

case class OrderNumUnitsQualifier(numUnits: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderNumUnits

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    if (input.eligibleForDiscountNumItems >= numUnits) accept()
    else reject(input, s"Order unit count is less than $numUnits")
}