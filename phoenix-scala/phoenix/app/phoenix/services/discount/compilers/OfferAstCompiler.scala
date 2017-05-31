package phoenix.services.discount.compilers

import cats.data.NonEmptyList
import cats.implicits._
import phoenix.failures.DiscountCompilerFailures._
import core.failures._
import phoenix.models.discount.offers._
import org.json4s._
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

case class OfferAstCompiler(data: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Either[Failures, Offer] = data match {
    case JObject(fields) ⇒ compile(fields)
    case _               ⇒ Either.left(OfferAstInvalidFormatFailure.single)
  }

  private def compile(fields: List[JField]): Either[Failures, Offer] = {
    val offerCompiles = fields.map {
      case (offerType, value) ⇒ compile(offerType, value)
    }

    val offers: Seq[Offer] = offerCompiles.flatMap { o ⇒
      o.fold(f ⇒ Seq.empty, q ⇒ Seq(q))
    }

    val failures = offerCompiles.flatMap { o ⇒
      o.fold(fs ⇒ fs.toList, q ⇒ Seq.empty)
    }

    failures match {
      case head :: tail ⇒ Either.left(NonEmptyList(head, tail))
      case Nil          ⇒ Either.right(OfferList(offers))
    }
  }

  private def compile(offerTypeString: String, attributes: Json): Either[Failures, Offer] =
    OfferType.read(offerTypeString) match {
      case Some(offerType) ⇒ OfferCompiler(offerType, attributes).compile()
      case _               ⇒ Either.left(OfferNotValid(offerTypeString).single)
    }
}
