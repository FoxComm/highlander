package models.discount.qualifiers

import models.discount.DiscountInput
import services.Result
import utils.aliases._

case class OrderNumUnitsQualifier(numUnits: Int) extends Qualifier {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] =
    if (input.lineItems.size > numUnits) accept() else reject(input, s"Order unit count is less than $numUnits")
}
