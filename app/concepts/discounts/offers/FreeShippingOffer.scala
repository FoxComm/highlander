package concepts.discounts.offers

import cats.data.Xor
import concepts.discounts._
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

case object FreeShippingOffer extends Offer {

  val rejectionReason = "Order has no shipping method"

  def adjust(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = shippingMethod match {

    case Some(sm) ⇒ Xor.Right(Seq(build(sm)))
    case _        ⇒ Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReason).single)
  }

  private def build(shippingMethod: ShippingMethod): LineItemAdjustment =
    LineItemAdjustment(lineItemType = ShippingLineItemType, substract = shippingMethod.price)
}
