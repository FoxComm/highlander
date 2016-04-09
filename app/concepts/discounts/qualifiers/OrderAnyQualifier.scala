package concepts.discounts.qualifiers

import cats.data.Xor
import concepts.discounts._
import failures.Failures
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

case object OrderAnyQualifier extends Qualifier {

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit] = Xor.Right(Unit)
}
