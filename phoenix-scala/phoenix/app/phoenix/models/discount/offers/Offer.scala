package phoenix.models.discount.offers

import cats.implicits._
import core.db._
import core.failures._
import phoenix.failures.DiscountFailures.SearchFailure
import phoenix.models.discount._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.ElasticsearchApi.Buckets
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

trait Offer extends DiscountBase {

  val offerType: OfferType

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis): Result[Seq[OfferResult]]

  def buildEither(input: DiscountInput,
                  subtract: Long,
                  lineItemRefNum: Option[String] = None): Either[Failures, Seq[OfferResult]] =
    Either.right(Seq(OfferResult(input, subtract, lineItemRefNum, offerType)))

  def buildResult(input: DiscountInput, subtract: Long, lineItemRefNum: Option[String] = None)(
      implicit ec: EC): Result[Seq[OfferResult]] =
    Result.good(Seq(OfferResult(input, subtract, lineItemRefNum, offerType)))

  def pureResult()(implicit ec: EC): Result[Seq[OfferResult]] = Result.good(Seq.empty)
  def pureEither(): Either[Failures, Seq[OfferResult]]        = Either.left(SearchFailure.single)
}

object Offer {
  case class OfferResult(discountInput: DiscountInput,
                         subtract: Long,
                         lineItemRefNum: Option[String],
                         offerType: OfferType)
}

/**
  * Offers that subtract amount from base price
  */
trait AmountOffer {

  // If discount amount is bigger than price - subtract price, otherwise subtract discount
  def subtract(price: Long, discount: Long): Long = {
    val delta = price - discount
    if (delta > 0) discount else price
  }
}

/**
  * Offers that subtract percent from base price
  */
trait PercentOffer {

  // Ceiling will give a discount bigger by one penny
  def subtract(price: Long, discount: Long): Long =
    Math.ceil((price * discount).toDouble / 100).toLong
}

/**
  * Offers that sets new price
  */
trait SetOffer {

  // If set value is bigger than price - subtract price, otherwise subtract delta to reach desired set value
  def subtract(price: Long, setPrice: Long): Long = {
    val delta = price - setPrice
    if (delta > 0) delta else price
  }
}

trait ItemsOffer {

  def matchEither(input: DiscountInput)(either: Either[Failures, Buckets]): Either[Failures, Seq[OfferResult]] // FIXME: why use matchEither instead of .map, if *never* do anything with Left? @michalrus

  def adjustInner(input: DiscountInput)(
      search: Seq[ProductSearch])(implicit db: DB, ec: EC, apis: Apis): Result[Seq[OfferResult]] = {
    val inAnyOf = search.map(_.query(input).mapEither(matchEither(input)))
    Result.onlySuccessful(inAnyOf.toList).map(_.headOption.getOrElse(Seq.empty))
  }
}
