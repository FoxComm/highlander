package concepts.discounts.qualifiers

import cats.data.Xor
import concepts.discounts._
import failures.Failures
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

final case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val rejectionReason = s"Order subtotal is less than $totalAmount"

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit] = {

    if (order.subTotal > totalAmount)
      Xor.Right(Unit)
    else
      Xor.Left(QualifierRejectionFailure(this, order.refNum, rejectionReason).single)
  }
}
