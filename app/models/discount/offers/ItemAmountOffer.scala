package models.discount.offers

import models.discount.{DiscountInput, ReferenceTuple}
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

case class ItemAmountOffer(discount: Int, references: Seq[ReferenceTuple]) extends Offer {

  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] =
    reject(input, "Not implemented yet")
}
