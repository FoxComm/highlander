package services.discount.compilers

import cats.data.NonEmptyList
import cats.implicits._
import failures.DiscountCompilerFailures._
import failures._
import io.circe.JsonObject
import models.discount.qualifiers._
import utils.aliases._

case class QualifierAstCompiler(data: Json) {

  def compile(): Either[Failures, Qualifier] = data.asObject match {
    case Some(obj) ⇒ compile(obj)
    case _         ⇒ Either.left(QualifierAstInvalidFormatFailure.single)
  }

  private def compile(obj: JsonObject): Either[Failures, Qualifier] = {
    val qualifierCompiles = obj.toVector.map {
      case (qualifierType, value) ⇒ compile(qualifierType, value)
    }

    val qualifiers: Seq[Qualifier] = qualifierCompiles.flatMap { o ⇒
      o.fold(f ⇒ Seq.empty, q ⇒ Seq(q))
    }

    val failures = qualifierCompiles.flatMap { o ⇒
      o.fold(fs ⇒ fs.toList, q ⇒ Seq.empty)
    }

    failures match {
      case head :: tail ⇒ Either.left(NonEmptyList(head, tail))
      case Nil          ⇒ Either.right(AndQualifier(qualifiers))
    }
  }

  private def compile(qualifierTypeString: String, attributes: Json): Either[Failures, Qualifier] =
    QualifierType.read(qualifierTypeString) match {
      case Some(qualifierType) ⇒ QualifierCompiler(qualifierType, attributes).compile()
      case _                   ⇒ Either.left(QualifierNotValid(qualifierTypeString).single)
    }
}
