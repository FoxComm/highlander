package services.discount.compilers

import cats.implicits._
import failures.DiscountCompilerFailures._
import failures._
import io.circe.Decoder
import models.discount.NonEmptySearch
import models.discount.qualifiers._
import utils.aliases._
import utils.json.codecs._

case class QualifierCompiler(qualifierType: QualifierType, attributes: Json) {

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

  private def extract[T <: Qualifier: Decoder](json: Json): Either[Failures, Qualifier] = {
    json.as[T] match {
      case Right(q) ⇒
        q match {
          case q: NonEmptySearch if q.search.isEmpty ⇒
            Either.left(QualifierSearchIsEmpty(qualifierType).single)
          case _ ⇒
            Either.right(q)
        }
      case Left(_) ⇒
        Either.left(QualifierAttributesExtractionFailure(qualifierType).single)
    }
  }
}
