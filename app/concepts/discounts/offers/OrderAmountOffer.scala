package concepts.discounts.offers

import cats.data.Xor
import Offer.AdjustmentResult
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment.OrderLineItemType
import models.shipping.ShippingMethod

case class OrderAmountOffer(discount: Int) extends Offer {

  val rejectionReason = "Invalid discount value provided (should be bigger than zero)"

  def adjust(order: Order, promoId: Int, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = {

    if (discount > 0) {
      val delta = order.grandTotal - discount
      val substract = if (delta > 0) delta else order.grandTotal
      Xor.Right(Seq(build(order, promoId, substract)))
    } else {
      Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReason).single)
    }
  }

  private def build(order: Order, promoId: Int, subtract: Int): OrderLineItemAdjustment =
    OrderLineItemAdjustment(orderId = order.id, promotionShadowId = promoId, adjustmentType = OrderLineItemType,
      subtract = subtract)
}
