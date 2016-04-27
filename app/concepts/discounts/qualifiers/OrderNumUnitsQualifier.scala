package concepts.discounts.qualifiers

import cats.data.Xor
import failures.Failures
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

case class OrderNumUnitsQualifier(numUnits: Int) extends Qualifier {

  val rejectionReason = s"Order unit count is less than $numUnits"

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit] = {

    if (lineItems.size > numUnits)
      Xor.Right(Unit)
    else
      Xor.Left(QualifierRejectionFailure(this, order.refNum, rejectionReason).single)
  }
}
