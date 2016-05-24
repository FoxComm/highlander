package models.discount.qualifiers

import models.discount.DiscountInput
import services._
import utils.aliases._

case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderTotalAmount

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] =
    if (input.order.subTotal >= totalAmount) accept()
    else reject(input, s"Order subtotal is less than $totalAmount")
}
