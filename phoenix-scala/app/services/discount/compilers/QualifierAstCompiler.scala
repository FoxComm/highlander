package services.discount.compilers

import cats.data.{NonEmptyList, Xor}
import cats.instances.list._
import failures.DiscountCompilerFailures._
import failures._
import models.discount.qualifiers._
import org.json4s._
import utils.JsonFormatters
import utils.aliases._

case class QualifierAstCompiler(data: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Qualifier] = data match {
    case JObject(fields) ⇒ compile(fields)
    case _               ⇒ Xor.Left(QualifierAstInvalidFormatFailure.single)
  }

  private def compile(fields: List[JField]): Xor[Failures, Qualifier] = {
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
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(AndQualifier(qualifiers))
    }
  }

  private def compile(qualifierTypeString: String, attributes: Json): Xor[Failures, Qualifier] =
    QualifierType.read(qualifierTypeString) match {
      case Some(qualifierType) ⇒ QualifierCompiler(qualifierType, attributes).compile()
      case _                   ⇒ Xor.Left(QualifierNotValid(qualifierTypeString).single)
    }
}
