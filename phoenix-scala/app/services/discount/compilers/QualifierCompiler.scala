package services.discount.compilers

import cats.data.Xor
import failures.DiscountCompilerFailures._
import failures._
import models.discount.NonEmptySearch
import models.discount.qualifiers._
import org.json4s._
import utils.JsonFormatters
import utils.aliases._

case class QualifierCompiler(qualifierType: QualifierType, attributes: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Qualifier] = qualifierType match {
    case And                  ⇒ extract[AndQualifier](attributes)
    case CustomerDynamicGroup ⇒ extract[CustomerDynamicGroupQualifier](attributes)
    case OrderAny             ⇒ Xor.Right(OrderAnyQualifier)
    case OrderTotalAmount     ⇒ extract[OrderTotalAmountQualifier](attributes)
    case OrderNumUnits        ⇒ extract[OrderNumUnitsQualifier](attributes)
    case ItemsAny             ⇒ extract[ItemsAnyQualifier](attributes)
    case ItemsTotalAmount     ⇒ extract[ItemsTotalAmountQualifier](attributes)
    case ItemsNumUnits        ⇒ extract[ItemsNumUnitsQualifier](attributes)
    case _                    ⇒ Xor.Left(QualifierNotImplementedFailure(qualifierType).single)
  }

  private def extract[T <: Qualifier](json: Json)(
      implicit m: Manifest[T]): Xor[Failures, Qualifier] = {
    json.extractOpt[T] match {
      case Some(q) ⇒
        q match {
          case q: NonEmptySearch if q.search.isEmpty ⇒
            Xor.Left(QualifierSearchIsEmpty(qualifierType).single)
          case _ ⇒ Xor.Right(q)
        }
      case None ⇒ Xor.Left(QualifierAttributesExtractionFailure(qualifierType).single)
    }
  }
}
