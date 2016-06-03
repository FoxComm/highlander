package models.discount.offers

import cats.data.Xor
import failures._
import models.discount.{DiscountBase, DiscountInput}
import models.discount.offers.Offer.OfferResult
import models.order.lineitems._
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

trait Offer extends DiscountBase {

  val offerType: OfferType

  val adjustmentType: AdjustmentType

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult

  // Returns single line item adjustment for now
  def build(input: DiscountInput,
            substract: Int,
            lineItemRefNum: Option[String] = None): OrderLineItemAdjustment =
    OrderLineItemAdjustment(orderId = input.order.id,
                            promotionShadowId = input.promotion.id,
                            adjustmentType = adjustmentType,
                            substract = substract,
                            lineItemRefNum = lineItemRefNum)

  def buildXor(input: DiscountInput,
               substract: Int,
               lineItemRefNum: Option[String] =
                 None): Xor[Failures, Seq[OrderLineItemAdjustment]] =
    Xor.Right(Seq(build(input, substract, lineItemRefNum)))

  def buildResult(
      input: DiscountInput, substract: Int, lineItemRefNum: Option[String] = None): OfferResult =
    Result.good(Seq(build(input, substract, lineItemRefNum)))

  def pure(): Result[Seq[OrderLineItemAdjustment]]           = Result.good(Seq.empty)
  def pureXor(): Xor[Failures, Seq[OrderLineItemAdjustment]] = Xor.Right(Seq.empty)
}

object Offer {

  type OfferResult = Result[Seq[OrderLineItemAdjustment]]
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
