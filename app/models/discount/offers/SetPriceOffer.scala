package models.discount.offers

import models.discount.DiscountInput
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services._
import utils.aliases._

case class SetPriceOffer(setPrice: Int) extends Offer {

  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] =
    reject(input, "Not implemented yet")
}
