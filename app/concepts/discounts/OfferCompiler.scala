package concepts.discounts

import offers._
import cats.data.Xor
import failures.DiscountCompilerFailures._
import failures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

case class OfferCompiler(offerType: OfferType, attributes: JObject) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Offer] = offerType match {
    case OrderPercentOff       ⇒ extract[OrderPercentOffer](attributes)
    case ItemsSelectPercentOff ⇒ extract[ItemsSelectPercentOffer](attributes)
    case FreeShipping          ⇒ Xor.Right(FreeShippingOffer)
    case _                     ⇒ Xor.Left(OfferNotImplementedFailure(offerType).single)
  }

  private def extract[T <: Offer](json: JValue)(implicit m: Manifest[T]): Xor[Failures, Offer] = json.extractOpt[T] match {
    case Some(q) => Xor.Right(q)
    case None    => Xor.Left(OfferAttributesExtractionFailure(offerType).single)
  }
}
