package concepts.discounts.offers

import cats.data.Xor
import Offer.AdjustmentResult
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import models.shipping.ShippingMethod

case class OrderPercentOffer(discount: Int) extends Offer {

  val rejectionReason = "Invalid discount value provided (should be between 1 and 99)"

  def adjust(order: Order, promoId: Int, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = {

    if (discount > 0 && discount < 100) {
      val amount = (order.subTotal * discount) / 100.0d
      val substract = Math.ceil(amount).toInt // This will give a bigger discount by one penny

      Xor.Right(Seq(build(order, promoId, substract)))
    } else {
      Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReason).single)
    }
  }

  private def build(order: Order, promoId: Int, subtract: Int): OrderLineItemAdjustment =
    OrderLineItemAdjustment(orderId = order.id, promotionShadowId = promoId, adjustmentType = OrderAdjustment,
      subtract = subtract)
}
