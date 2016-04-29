package concepts.discounts.offers

import cats.data.Xor
import Offer.AdjustmentResult
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment.OrderAdjustment
import models.shipping.ShippingMethod

case class SetPriceOffer(setPrice: Int) extends Offer {

  val rejectionReason = "Invalid set price value provided (should be bigger than zero)"

  def adjust(order: Order, promoId: Int, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = {

    if (setPrice > 0) {
      val delta = order.subTotal - setPrice
      val substract = if (delta > 0) setPrice else order.subTotal
      Xor.Right(Seq(build(order, promoId, substract)))
    } else {
      Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReason).single)
    }
  }

  private def build(order: Order, promoId: Int, subtract: Int): OrderLineItemAdjustment =
    OrderLineItemAdjustment(orderId = order.id, promotionShadowId = promoId, adjustmentType = OrderAdjustment,
      subtract = subtract)
}
