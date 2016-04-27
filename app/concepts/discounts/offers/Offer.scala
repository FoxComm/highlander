package concepts.discounts.offers

import cats.data.Xor
import concepts.discounts.offers.Offer.AdjustmentResult
import failures._
import models.order.Order
import models.order.lineitems._
import models.shipping.ShippingMethod

trait Offer {

  def adjust(order: Order, promoId: Int, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult
}

object Offer {

  type AdjustmentResult = Xor[Failures, Seq[OrderLineItemAdjustment]]

}
