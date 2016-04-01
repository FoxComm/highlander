package concepts.discounts.offers

import cats.data.Xor
import Offer.AdjustmentResult
import concepts.discounts._
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

final case class OrderPercentOffer(discount: Int) extends Offer {

  val rejectionReason = "Invalid discount value provided (should be between 1 and 99)"

  def adjust(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = {

    // Should we include 100% discount or make FreeItemOffer?
    if (discount > 0 && discount < 100) {
      // TODO: Use proper Decimal library
      val amount = (order.subTotal / 100.0d) * discount
      val substract = amount.toInt
      val pennies = amount - substract

      Xor.Right(Seq(build(substract, pennies)))
    } else {
      Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReason).single)
    }
  }

  private def build(substract: Int, pennies: Double): LineItemAdjustment =
    LineItemAdjustment(lineItemType = OrderLineItemType, substract = substract, pennies = pennies)
}
