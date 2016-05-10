package services.discount.compilers

import cats.data.Xor
import failures.DiscountCompilerFailures._
import failures._
import models.discount.qualifiers._
import org.json4s._
import utils.JsonFormatters

case class QualifierCompiler(qualifierType: QualifierType, attributes: JValue) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Qualifier] = qualifierType match {
    case And              ⇒ extract[AndQualifier](attributes)
    case OrderAny         ⇒ Xor.Right(OrderAnyQualifier)
    case OrderTotalAmount ⇒ extract[OrderTotalAmountQualifier](attributes)
    case OrderNumUnits    ⇒ extract[OrderNumUnitsQualifier](attributes)
    case ItemsAny         ⇒ extract[ItemsAnyQualifier](attributes)
    case ItemsTotalAmount ⇒ extract[ItemsTotalAmountQualifier](attributes)
    case ItemsNumUnits    ⇒ extract[ItemsNumUnitsQualifier](attributes)
    case _                ⇒ Xor.Left(QualifierNotImplementedFailure(qualifierType).single)
  }

  private def extract[T <: Qualifier](json: JValue)(implicit m: Manifest[T]): Xor[Failures, Qualifier] = {
    json.extractOpt[T] match {
      case Some(q) ⇒ Xor.Right(q)
      case None    ⇒ Xor.Left(QualifierAttributesExtractionFailure(qualifierType).single)
    }
  }
}
