package services.discount.compilers

import cats.data.Xor
import failures.DiscountCompilerFailures._
import failures._
import models.discount.NonEmptySearch
import models.discount.offers._
import org.json4s._
import utils.JsonFormatters
import utils.aliases._

case class OfferCompiler(offerType: OfferType, attributes: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Offer] = offerType match {
    case OrderPercentOff    ⇒ extract[OrderPercentOffer](attributes)
    case OrderAmountOff     ⇒ extract[OrderAmountOffer](attributes)
    case ItemPercentOff     ⇒ extract[ItemPercentOffer](attributes)
    case ItemAmountOff      ⇒ extract[ItemAmountOffer](attributes)
    case ItemsPercentOff    ⇒ extract[ItemsPercentOffer](attributes)
    case ItemsAmountOff     ⇒ extract[ItemsAmountOffer](attributes)
    case FreeShipping       ⇒ Xor.Right(FreeShippingOffer)
    case DiscountedShipping ⇒ extract[DiscountedShippingOffer](attributes)
    case SetPrice           ⇒ extract[SetPriceOffer](attributes)
    case _                  ⇒ Xor.Left(OfferNotImplementedFailure(offerType).single)
  }

  private def extract[T <: Offer](json: Json)(implicit m: Manifest[T]): Xor[Failures, Offer] =
    json.extractOpt[T] match {
      case Some(q) ⇒
        q match {
          case q: NonEmptySearch if q.search.isEmpty ⇒
            Xor.Left(OfferSearchIsEmpty(offerType).single)
          case _ ⇒
            Xor.Right(q)
        }
      case None ⇒
        Xor.Left(OfferAttributesExtractionFailure(offerType).single)
    }
}
