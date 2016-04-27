package concepts.discounts.qualifiers

import cats.data.Xor
import concepts.discounts.ReferenceTuple
import failures.Failures
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

case class ItemsAnyQualifier(references: Seq[ReferenceTuple]) extends Qualifier {

  val rejectionReason = "Not implemented yet"

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit] = {

    Xor.Left(QualifierRejectionFailure(this, order.refNum, rejectionReason).single)
  }
}
