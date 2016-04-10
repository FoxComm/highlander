package concepts.discounts

import cats.data.Xor
import concepts.discounts.offers.Offer._
import offers._
import failures._
import failures.DiscountCompilerFailures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

final case class OfferAstCompiler(input: String) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Offer] = parseOpt(input) match {
    case Some(json) ⇒ compileInner(json)
    case _          ⇒ Xor.Left(OfferAstParseFailure(input).single)
  }

  private def compileInner(json: JValue): Xor[Failures, Offer] = json.extractOpt[OfferAstFormat] match {
    // Extract first element, currently
    case Some(q) ⇒ q.headOption match {
      case Some(OfferFormat(offerType, attributes)) ⇒ OfferCompiler(offerType, attributes).compile()
      case _                                        ⇒ Xor.Left(OfferAstEmptyObjectFailure.single)
    }
    case _       ⇒ Xor.Left(OfferAstInvalidFormatFailure.single)
  }
}
