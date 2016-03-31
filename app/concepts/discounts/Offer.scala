package concepts.discounts

import cats.data.Xor
import failures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

trait Offer {

  type AdjustmentResult = Xor[Failures, Seq[LineItemAdjustment]]

  def adjust(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult
}
