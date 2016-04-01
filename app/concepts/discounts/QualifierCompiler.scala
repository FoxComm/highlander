package concepts.discounts

import qualifiers._
import cats.data.Xor
import failures._
import failures.DiscountCompilerFailures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

final case class QualifierCompiler(qualifierType: String, attributes: String) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Qualifier] = (QualifierType.read(qualifierType), parseOpt(attributes)) match {
    case (Some(q), Some(j)) ⇒ compileInner(q, j)
    case (_, Some(j))       ⇒ Xor.Left(UnknownQualifierFailure(qualifierType).single)
    case (_, _)             ⇒ Xor.Left(QualifierAttributesParseFailure(qualifierType, attributes).single)
  }

  private def compileInner(qualifierAdt: QualifierType, json: JValue): Xor[Failures, Qualifier] = qualifierAdt match {
    case OrderAny         ⇒ Xor.Right(OrderAnyQualifier)
    case OrderTotalAmount ⇒ extract[OrderTotalAmountQualifier](json)
    case ItemsAny         ⇒ Xor.Right(ItemsAnyQualifier)
    case _                ⇒ Xor.Left(QualifierNotImplementedFailure(qualifierType).single)
  }

  private def extract[T <: Qualifier](json: JValue)(implicit m: Manifest[T]): Xor[Failures, Qualifier] = {
    json.extractOpt[T] match {
      case Some(q) ⇒ Xor.Right(q)
      case None    ⇒ Xor.Left(QualifierAttributesExtractionFailure(qualifierType, attributes).single)
    }
  }
}
