package concepts.discounts

import qualifiers._
import cats.data.Xor
import failures._
import failures.DiscountCompilerFailures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

final case class QualifierCompiler(qualifierType: String, attributes: String) {

  implicit val formats: Formats = JsonFormatters.DefaultFormats

  def compile(): Xor[Failure, Qualifier] = (QualifierType.read(qualifierType), parseOpt(attributes)) match {
    case (Some(q), Some(j)) ⇒ compileInner(q, j)
    case (_, Some(j))       ⇒ Xor.Left(UnknownQualifierFailure(qualifierType))
    case (_, _)             ⇒ Xor.Left(QualifierAttributesParseFailure(qualifierType, attributes))
  }

  private def compileInner(qualifierAdt: QualifierType, json: JValue): Xor[Failure, Qualifier] = qualifierAdt match {
    case OrderAny         ⇒ Xor.Right(OrderAnyQualifier)
    case OrderTotalAmount ⇒ extract[OrderTotalAmountQualifier](json)
    case ItemsAny         ⇒ Xor.Right(ItemsAnyQualifier)
    case _                ⇒ Xor.Left(QualifierNotImplementedFailure(qualifierType))
  }

  private def extract[T <: Qualifier](json: JValue)(implicit m: Manifest[T]): Xor[Failure, Qualifier] = json.extractOpt[T] match {
    case Some(q) => Xor.Right(q)
    case None    => Xor.Left(QualifierAttributesExtractionFailure(qualifierType, attributes))
  }
}
