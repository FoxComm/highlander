package concepts.discounts.qualifiers

import cats.data.Xor
import concepts.discounts._
import failures.Failures
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

case object ItemsAnyQualifier extends Qualifier {

  val rejectionReason = "Order has no line items"

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit] = {

    if (lineItems.nonEmpty)
      Xor.Right(Unit)
    else
      Xor.Left(QualifierRejectionFailure(this, order.refNum, rejectionReason).single)
  }
}
