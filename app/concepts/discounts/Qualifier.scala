package concepts.discounts

import cats.data.Xor
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod
import failures._

trait Qualifier {

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit]
}
