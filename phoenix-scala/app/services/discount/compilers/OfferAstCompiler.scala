package services.discount.compilers

import cats.data.NonEmptyList
import cats.implicits._
import failures.DiscountCompilerFailures._
import failures._
import io.circe.JsonObject
import models.discount.offers._
import utils.aliases._

case class OfferAstCompiler(data: Json) {

  def compile(): Either[Failures, Offer] = data.asObject match {
    case Some(obj) ⇒ compile(obj)
    case _         ⇒ Either.left(OfferAstInvalidFormatFailure.single)
  }

  private def compile(obj: JsonObject): Either[Failures, Offer] = {
    val offerCompiles = obj.toList.map {
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
