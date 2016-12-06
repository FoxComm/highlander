package services.discount.compilers

import cats.data.{NonEmptyList, Xor}
import cats.instances.list._
import failures.DiscountCompilerFailures._
import failures._
import models.discount.offers._
import org.json4s._
import utils.JsonFormatters
import utils.aliases._

case class OfferAstCompiler(data: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Offer] = data match {
    case JObject(fields) ⇒ compile(fields)
    case _               ⇒ Xor.Left(OfferAstInvalidFormatFailure.single)
  }

  private def compile(fields: List[JField]): Xor[Failures, Offer] = {
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
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(OfferList(offers))
    }
  }

  private def compile(offerTypeString: String, attributes: Json): Xor[Failures, Offer] =
    OfferType.read(offerTypeString) match {
      case Some(offerType) ⇒ OfferCompiler(offerType, attributes).compile()
      case _               ⇒ Xor.Left(OfferNotValid(offerTypeString).single)
    }
}
