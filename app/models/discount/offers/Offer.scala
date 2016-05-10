package models.discount.offers

import failures.DiscountCompilerFailures.OfferRejectionFailure
import models.discount.DiscountInput
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

trait Offer {

  val adjustmentType: AdjustmentType

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]]

  // Returns single line item adjustment
  def accept(input: DiscountInput, substract: Int, lineItemId: Option[Int] = None)
    (implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] = {

    val adj = OrderLineItemAdjustment(orderId = input.order.id, promotionShadowId = input.promotion.id,
      adjustmentType = adjustmentType, substract = substract, lineItemId = lineItemId)

    Result.good(Seq(adj))
  }

  def reject(input: DiscountInput, message: String): Result[Seq[OrderLineItemAdjustment]] =
    Result.failure(OfferRejectionFailure(this, input.order.refNum, message))
}
