package models.discount.offers

import models.discount.DiscountInput
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services._
import utils.aliases._

case class OrderPercentOffer(discount: Int) extends Offer {

  val adjustmentType: AdjustmentType = OrderAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] = {
    if (discount > 0 && discount < 100) {
      val amount = (input.order.subTotal * discount) / 100.0d
      val substract = Math.ceil(amount).toInt // This will give a bigger discount by one penny
      accept(input, substract)
    } else {
      reject(input, "Invalid discount value provided (should be between 1 and 99)")
    }
  }
}
