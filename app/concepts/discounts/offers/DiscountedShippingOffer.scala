package concepts.discounts.offers

import cats.data.Xor
import Offer.AdjustmentResult
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment.ShippingLineItemType
import models.shipping.ShippingMethod

case class DiscountedShippingOffer(setPrice: Int) extends Offer {

  val rejectionReasonNoShippingMethod = "Order has no shipping method"
  val rejectionReasonSetPriceNegative = "Invalid set price value provided (should be bigger than zero)"

  def adjust(order: Order, promoId: Int, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = shippingMethod match {

    case Some(sm) ⇒
      if (setPrice > 0) {
        val delta = sm.price - setPrice
        val substract = if (delta > 0) delta else sm.price
        Xor.Right(Seq(build(order, promoId, substract)))
      } else {
        Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReasonSetPriceNegative).single)
      }
    case _        ⇒
      Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReasonNoShippingMethod).single)
  }

  private def build(order: Order, promoId: Int, substract: Int): OrderLineItemAdjustment =
    OrderLineItemAdjustment(orderId = order.id, promotionShadowId = promoId, adjustmentType = ShippingLineItemType,
      subtract = substract)
}
