package phoenix.models.discount.offers

import cats.implicits._
import core.failures._
import phoenix.failures.DiscountFailures.SearchFailure
import phoenix.models.cord.lineitems.CartLineItemAdjustment._
import phoenix.models.cord.lineitems.{CartLineItemAdjustment ⇒ Adjustment}
import phoenix.models.discount._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.ElasticsearchApi.Buckets
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import utils.db._

trait Offer extends DiscountBase {

  val offerType: OfferType

  val adjustmentType: AdjustmentType

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult

  // Returns single line item adjustment for now
  def build(input: DiscountInput,
            subtract: Int,
            lineItemRefNum: Option[String] = None): Adjustment =
    Adjustment(cordRef = input.cart.refNum,
               promotionShadowId = input.promotion.id,
               adjustmentType = adjustmentType,
               subtract = subtract,
               lineItemRefNum = lineItemRefNum)

  def buildEither(input: DiscountInput,
                  subtract: Int,
                  lineItemRefNum: Option[String] = None): Either[Failures, Seq[Adjustment]] =
    Either.right(Seq(build(input, subtract, lineItemRefNum)))

  def buildResult(input: DiscountInput, subtract: Int, lineItemRefNum: Option[String] = None)(
      implicit ec: EC): OfferResult =
    Result.good(Seq(build(input, subtract, lineItemRefNum)))

  def pureResult()(implicit ec: EC): Result[Seq[Adjustment]] = Result.good(Seq.empty)
  def pureEither(): Either[Failures, Seq[Adjustment]]        = Either.left(SearchFailure.single)
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

  def matchEither(input: DiscountInput)(either: Either[Failures, Buckets])
    : Either[Failures, Seq[Adjustment]] // FIXME: why use matchEither instead of .map, if *never* do anything with Left? @michalrus

  def adjustInner(input: DiscountInput)(
      search: Seq[ProductSearch])(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult = {
    val inAnyOf = search.map(_.query(input).mapEither(matchEither(input)))
    Result.onlySuccessful(inAnyOf.toList).map(_.headOption.getOrElse(Seq.empty))
  }
}
