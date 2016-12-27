package models.discount.qualifiers

import models.discount.DiscountInput
import services._
import utils.aliases._

case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderTotalAmount

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    if (input.cart.subTotal >= totalAmount) accept()
    else reject(input, s"Order subtotal is less than $totalAmount")
}
