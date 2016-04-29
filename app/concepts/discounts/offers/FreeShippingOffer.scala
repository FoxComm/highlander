package concepts.discounts.offers

import cats.data.Xor
import Offer.AdjustmentResult
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import models.shipping.ShippingMethod

case object FreeShippingOffer extends Offer {

  val rejectionReason = "Order has no shipping method"

  def adjust(order: Order, promoId: Int, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = shippingMethod match {

    case Some(sm) ⇒ Xor.Right(Seq(build(order, promoId, sm)))
    case _        ⇒ Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReason).single)
  }

  private def build(order: Order, promoId: Int, shippingMethod: ShippingMethod): OrderLineItemAdjustment =
    OrderLineItemAdjustment(orderId = order.id, promotionShadowId = promoId, adjustmentType = ShippingAdjustment,
      subtract = shippingMethod.price)
}
