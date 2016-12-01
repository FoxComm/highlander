package models.discount.qualifiers

import models.account.User
import models.discount.DiscountInput
import services.Authenticator.AuthData
import services._
import utils.aliases._

case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderTotalAmount

  def check(
      input: DiscountInput)(implicit db: DB, ec: EC, es: ES, auth: AuthData[User]): Result[Unit] =
    if (input.cart.subTotal >= totalAmount) accept()
    else reject(input, s"Order subtotal is less than $totalAmount")
}
