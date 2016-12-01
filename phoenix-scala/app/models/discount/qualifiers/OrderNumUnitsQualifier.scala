package models.discount.qualifiers

import models.account.User
import models.discount.DiscountInput
import services.Authenticator.AuthData
import services.Result
import utils.aliases._

case class OrderNumUnitsQualifier(numUnits: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderNumUnits

  def check(
      input: DiscountInput)(implicit db: DB, ec: EC, es: ES, auth: AuthData[User]): Result[Unit] =
    if (input.lineItems.size >= numUnits) accept()
    else reject(input, s"Order unit count is less than $numUnits")
}
