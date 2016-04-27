package concepts.discounts

import cats.data.Xor
import offers._
import failures._
import failures.DiscountCompilerFailures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters
import cats.data.NonEmptyList
import cats.std.list._

case class OfferAstCompiler(input: String) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats
  def compile(): Xor[Failures, Offer] = parseOpt(input) match {
    case Some(json) ⇒ compile(json)
    case _          ⇒ Xor.Left(OfferAstParseFailure(input).single)
  }

  private def compile(data: JValue): Xor[Failures, Offer] = {
    data match {
      case JObject(fields) ⇒ compile(fields)
      case _               ⇒ Xor.Left(OfferAstInvalidFormatFailure.single)
    }
  }

  private def compile(fields: List[JField]) : Xor[Failures, Offer] = {
    val offerCompiles = fields.map {
      case (offerType, value) ⇒ compile(offerType, value)
    }

    val offers: Seq[Offer] = offerCompiles.flatMap { 
      o ⇒ o.fold(f ⇒ Seq.empty, q ⇒ Seq(q))
    }

    val failures = offerCompiles.flatMap { 
      o ⇒ o.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty)
    }

    failures match {
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(OfferList(offers))
    }
  }

  private def compile(offerTypeString: String, attributes: JValue): Xor[Failures, Offer] = 
    OfferType.read(offerTypeString) match {
      case Some(offerType) ⇒ OfferCompiler(offerType, attributes).compile()
      case _               ⇒ Xor.Left(OfferNotValid(offerTypeString).single)
    }
}
