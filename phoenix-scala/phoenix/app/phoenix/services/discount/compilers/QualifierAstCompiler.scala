package phoenix.services.discount.compilers

import cats.data.NonEmptyList
import cats.implicits._
import phoenix.failures.DiscountCompilerFailures._
import core.failures._
import phoenix.models.discount.qualifiers._
import org.json4s._
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

case class QualifierAstCompiler(data: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Either[Failures, Qualifier] = data match {
    case JObject(fields) ⇒ compile(fields)
    case _               ⇒ Either.left(QualifierAstInvalidFormatFailure.single)
  }

  private def compile(fields: List[JField]): Either[Failures, Qualifier] = {
    val qualifierCompiles = fields.map {
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
