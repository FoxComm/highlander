package models.discount.offers

import models.discount.DiscountInput
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

case object FreeShippingOffer extends Offer {

  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] = {
    input.shippingMethod match {
      case Some(sm) ⇒ accept(input, sm.price)
      case _        ⇒ reject(input, "Order has no shipping method")
    }
  }
}
