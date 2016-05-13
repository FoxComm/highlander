package models.discount.offers

import cats.data.Xor
import failures._
import failures.DiscountCompilerFailures.OfferRejectionFailure
import models.discount.{DiscountBase, DiscountInput}
import models.discount.offers.Offer.OfferResult
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._

trait Offer extends DiscountBase {

  val adjustmentType: AdjustmentType

  def adjust(input: DiscountInput): OfferResult

  // Returns single line item adjustment for now
  def accept(input: DiscountInput, substract: Int, lineItemId: Option[Int] = None): OfferResult = {

    val adj = OrderLineItemAdjustment(orderId = input.order.id, promotionShadowId = input.promotion.id,
      adjustmentType = adjustmentType, substract = substract, lineItemId = lineItemId)

    Xor.Right(Seq(adj))
  }

  def reject(input: DiscountInput, message: String): OfferResult =
    Xor.Left(OfferRejectionFailure(this, input, message).single)
}

object Offer {

  type OfferResult = Failures Xor Seq[OrderLineItemAdjustment]
}

/**
  * Offers that substract amount from base price
  */
trait AmountOffer {

  // If discount amount is bigger than price - substract price, otherwise substract discount
  def substract(price: Int, discount: Int): Int = {
    val delta = price - discount
    if (delta > 0) discount else price
  }
}

/**
 * Offers that substract percent from base price
 */
trait PercentOffer {

  // Ceiling will give a discount bigger by one penny
  def substract(price: Int, discount: Int): Int = Math.ceil((price * discount) / 100.0d).toInt
}

/**
  * Offers that sets new price
  */
trait SetOffer {

  // If set value is bigger than price - substract price, otherwise substract delta to reach desired set value
  def substract(price: Int, setPrice: Int): Int = {
    val delta = price - setPrice
    if (delta > 0) delta else price
  }
}
