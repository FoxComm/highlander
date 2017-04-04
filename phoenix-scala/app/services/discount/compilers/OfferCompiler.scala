package services.discount.compilers

import cats.implicits._
import failures.DiscountCompilerFailures._
import failures._
import models.discount.NonEmptySearch
import models.discount.offers._
import org.json4s._
import utils.JsonFormatters
import utils.aliases._

case class OfferCompiler(offerType: OfferType, attributes: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Either[Failures, Offer] = offerType match {
    case OrderPercentOff    ⇒ extract[OrderPercentOffer](attributes)
    case OrderAmountOff     ⇒ extract[OrderAmountOffer](attributes)
    case ItemPercentOff     ⇒ extract[ItemPercentOffer](attributes)
    case ItemAmountOff      ⇒ extract[ItemAmountOffer](attributes)
    case ItemsPercentOff    ⇒ extract[ItemsPercentOffer](attributes)
    case ItemsAmountOff     ⇒ extract[ItemsAmountOffer](attributes)
    case FreeShipping       ⇒ Either.right(FreeShippingOffer)
    case DiscountedShipping ⇒ extract[DiscountedShippingOffer](attributes)
    case SetPrice           ⇒ extract[SetPriceOffer](attributes)
    case _                  ⇒ Either.left(OfferNotImplementedFailure(offerType).single)
  }

  private def extract[T <: Offer](json: Json)(implicit m: Manifest[T]): Either[Failures, Offer] =
    try {
      json.extract[T] match {
        case q: NonEmptySearch if q.search.isEmpty ⇒
          Either.left(OfferSearchIsEmpty(offerType).single)
        case q ⇒
          Either.right(q)
      }
    } catch {
      case e: MappingException ⇒
        Either.left(OfferAttributesExtractionFailure(offerType, e).single)
    }
}
