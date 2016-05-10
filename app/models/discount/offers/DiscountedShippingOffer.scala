package models.discount.offers

import models.discount.DiscountInput
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

case class DiscountedShippingOffer(setPrice: Int) extends Offer {

  val adjustmentType: AdjustmentType = ShippingAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] = {
    input.shippingMethod match {
      case Some(sm) ⇒
        if (setPrice > 0) {
          val delta = sm.price - setPrice
          val substract = if (delta > 0) delta else sm.price
          accept(input, substract)
        } else {
          reject(input, "Order has no shipping method")
        }
      case _ ⇒
        reject(input, "Invalid set price value provided (should be bigger than zero)")
    }
  }
}
