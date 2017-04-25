package services.discount.compilers

import cats.implicits._
import failures.DiscountCompilerFailures._
import failures._
import io.circe.Decoder
import models.discount.NonEmptySearch
import models.discount.offers._
import utils.aliases._

case class OfferCompiler(offerType: OfferType, attributes: Json) {

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

  private def extract[T <: Offer: Decoder](json: Json): Either[Failures, Offer] =
    json.as[T] match {
      case Right(q) ⇒
        q match {
          case q: NonEmptySearch if q.search.isEmpty ⇒
            Either.left(OfferSearchIsEmpty(offerType).single)
          case _ ⇒
            Either.right(q)
        }
      case Left(_) ⇒
        Either.left(OfferAttributesExtractionFailure(offerType).single)
    }
}
