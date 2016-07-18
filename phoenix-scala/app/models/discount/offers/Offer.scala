package models.discount.offers

import scala.concurrent.Future

import cats.data.Xor
import failures.DiscountFailures.SearchFailure
import failures._
import models.cord.lineitems.OrderLineItemAdjustment._
import models.cord.lineitems._
import models.discount.offers.Offer.OfferResult
import models.discount.{DiscountBase, DiscountInput, ProductSearch}
import services.Result
import utils.ElasticsearchApi.{apply ⇒ _, _}
import utils.aliases._

trait Offer extends DiscountBase {

  val offerType: OfferType

  val adjustmentType: AdjustmentType

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult

  // Returns single line item adjustment for now
  def build(input: DiscountInput,
            substract: Int,
            lineItemRefNum: Option[String] = None): OrderLineItemAdjustment =
    OrderLineItemAdjustment(cordRef = input.cart.refNum,
                            promotionShadowId = input.promotion.id,
                            adjustmentType = adjustmentType,
                            substract = substract,
                            lineItemRefNum = lineItemRefNum)

  def buildXor(
      input: DiscountInput,
      substract: Int,
      lineItemRefNum: Option[String] = None): Xor[Failures, Seq[OrderLineItemAdjustment]] =
    Xor.Right(Seq(build(input, substract, lineItemRefNum)))

  def buildResult(input: DiscountInput,
                  substract: Int,
                  lineItemRefNum: Option[String] = None): OfferResult =
    Result.good(Seq(build(input, substract, lineItemRefNum)))

  def pureResult(): Result[Seq[OrderLineItemAdjustment]]     = Result.good(Seq.empty)
  def pureXor(): Xor[Failures, Seq[OrderLineItemAdjustment]] = Xor.Left(SearchFailure.single)
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

trait ItemsOffer {

  def matchXor(input: DiscountInput)(
      xor: Failures Xor Buckets): Failures Xor Seq[OrderLineItemAdjustment]

  def adjustInner(input: DiscountInput)(
      search: Seq[ProductSearch])(implicit db: DB, ec: EC, es: ES): OfferResult = {
    val inAnyOf = search.map(_.query(input).map(matchXor(input)))

    Future
      .sequence(inAnyOf)
      .flatMap(xorSequence ⇒
            xorSequence.find(_.isRight) match {
          case Some(Xor.Right(adj)) ⇒ Result.good(adj)
          case _                    ⇒ Result.good(Seq.empty)
      })
  }
}
