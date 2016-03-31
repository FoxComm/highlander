package concepts.discounts

import offers._
import cats.data.Xor
import failures.DiscountCompilerFailures._
import failures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

final case class OfferCompiler(offerType: String, attributes: String) {

  implicit val formats: Formats = JsonFormatters.DefaultFormats

  def compile(): Xor[Failures, Offer] = (OfferType.read(offerType), parseOpt(attributes)) match {
    case (Some(q), Some(j)) ⇒ compileInner(q, j)
    case (_, Some(j))       ⇒ Xor.Left(UnknownOfferFailure(offerType).single)
    case (_, _)             ⇒ Xor.Left(OfferAttributesParseFailure(offerType, attributes).single)
  }

  private def compileInner(offerAdt: OfferType, json: JValue): Xor[Failures, Offer] = offerAdt match {
    case OrderPercentOff       ⇒ extract[OrderPercentOffer](json)
    case ItemsSelectPercentOff ⇒ extract[ItemsSelectPercentOffer](json)
    case FreeShipping          ⇒ Xor.Right(FreeShippingOffer)
    case _                     ⇒ Xor.Left(OfferNotImplementedFailure(offerType).single)
  }

  private def extract[T <: Offer](json: JValue)(implicit m: Manifest[T]): Xor[Failures, Offer] = json.extractOpt[T] match {
    case Some(q) => Xor.Right(q)
    case None    => Xor.Left(OfferAttributesExtractionFailure(offerType, attributes).single)
  }
}
