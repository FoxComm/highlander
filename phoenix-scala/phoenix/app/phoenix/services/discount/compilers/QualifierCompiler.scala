package phoenix.services.discount.compilers

import cats.implicits._
import phoenix.failures.DiscountCompilerFailures._
import core.failures._
import phoenix.models.discount.NonEmptySearch
import phoenix.models.discount.qualifiers._
import org.json4s._
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

case class QualifierCompiler(qualifierType: QualifierType, attributes: Json) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Either[Failures, Qualifier] = qualifierType match {
    case And                  ⇒ extract[AndQualifier](attributes)
    case CustomerDynamicGroup ⇒ extract[CustomerDynamicGroupQualifier](attributes)
    case OrderAny             ⇒ Either.right(OrderAnyQualifier)
    case OrderTotalAmount     ⇒ extract[OrderTotalAmountQualifier](attributes)
    case OrderNumUnits        ⇒ extract[OrderNumUnitsQualifier](attributes)
    case ItemsAny             ⇒ extract[ItemsAnyQualifier](attributes)
    case ItemsTotalAmount     ⇒ extract[ItemsTotalAmountQualifier](attributes)
    case ItemsNumUnits        ⇒ extract[ItemsNumUnitsQualifier](attributes)
    case _                    ⇒ Either.left(QualifierNotImplementedFailure(qualifierType).single)
  }

  private def extract[T <: Qualifier](json: Json)(implicit m: Manifest[T]): Either[Failures, Qualifier] =
    try {
      json.extract[T] match {
        case q: NonEmptySearch if q.search.isEmpty ⇒
          Either.left(QualifierSearchIsEmpty(qualifierType).single)
        case q ⇒
          Either.right(q)
      }
    } catch {
      case e: MappingException ⇒
        Either.left(QualifierAttributesExtractionFailure(qualifierType, e.getMessage).single)
    }
}
