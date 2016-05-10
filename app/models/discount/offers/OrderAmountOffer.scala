package models.discount.offers

import models.discount.DiscountInput
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

case class OrderAmountOffer(discount: Int) extends Offer {

  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] = {
    if (discount > 0) {
      val delta = input.order.subTotal - discount
      val substract = if (delta > 0) discount else input.order.subTotal
      accept(input, substract)
    } else {
      reject(input, "Invalid discount value provided (should be bigger than zero)")
    }
  }
}
