package models.discount.offers

import scala.concurrent.Future

import cats._
import cats.data._
import cats.implicits._
import failures.DiscountFailures.SearchFailure
import failures._
import models.cord.lineitems.OrderLineItemAdjustment._
import models.cord.lineitems.{OrderLineItemAdjustment ⇒ Adjustment}
import models.discount._
import models.discount.offers.Offer.OfferResult
import services.Result
import utils.ElasticsearchApi.Buckets
import utils.aliases._

trait Offer extends DiscountBase {

  val offerType: OfferType

  val adjustmentType: AdjustmentType

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult

  // Returns single line item adjustment for now
  def build(input: DiscountInput,
            subtract: Int,
            lineItemRefNum: Option[String] = None): Adjustment =
    Adjustment(cordRef = input.cart.refNum,
               promotionShadowId = input.promotion.id,
               adjustmentType = adjustmentType,
               subtract = subtract,
               lineItemRefNum = lineItemRefNum)

  def buildXor(input: DiscountInput,
               subtract: Int,
               lineItemRefNum: Option[String] = None): Xor[Failures, Seq[Adjustment]] =
    Xor.Right(Seq(build(input, subtract, lineItemRefNum)))

  def buildResult(input: DiscountInput, subtract: Int, lineItemRefNum: Option[String] = None)(
      implicit ec: EC): OfferResult =
    Result.good(Seq(build(input, subtract, lineItemRefNum)))

  def pureResult()(implicit ec: EC): Result[Seq[Adjustment]] = Result.good(Seq.empty)
  def pureXor(): Xor[Failures, Seq[Adjustment]]              = Xor.Left(SearchFailure.single)
}

object Offer {

  type OfferResult = Result[Seq[Adjustment]]
}

/**
  * Offers that subtract amount from base price
  */
trait AmountOffer {

  // If discount amount is bigger than price - subtract price, otherwise subtract discount
  def subtract(price: Int, discount: Int): Int = {
    val delta = price - discount
    if (delta > 0) discount else price
  }
}

/**
  * Offers that subtract percent from base price
  */
trait PercentOffer {

  // Ceiling will give a discount bigger by one penny
  def subtract(price: Int, discount: Int): Int = Math.ceil((price * discount) / 100.0d).toInt
}

/**
  * Offers that sets new price
  */
trait SetOffer {

  // If set value is bigger than price - subtract price, otherwise subtract delta to reach desired set value
  def subtract(price: Int, setPrice: Int): Int = {
    val delta = price - setPrice
    if (delta > 0) delta else price
  }
}

trait ItemsOffer {

  def matchXor(input: DiscountInput)(xor: Failures Xor Buckets): Failures Xor Seq[Adjustment] // FIXME: why use matchXor instead of .map, if *never* do anything with Left? @michalrus

  def adjustInner(input: DiscountInput)(
      search: Seq[ProductSearch])(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult = {
    val inAnyOf = search.map(_.query(input).mapXor(matchXor(input)))
    Result.onlySuccessful(inAnyOf).map(_.headOption.getOrElse(Seq.empty))
  }
}
