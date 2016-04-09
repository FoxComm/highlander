package concepts.discounts

import qualifiers._
import cats.data.Xor
import failures._
import failures.DiscountCompilerFailures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

final case class QualifierCompiler(qualifierType: QualifierType, attributes: JObject) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Qualifier] = qualifierType match {
    case OrderAny         ⇒ Xor.Right(OrderAnyQualifier)
    case OrderTotalAmount ⇒ extract[OrderTotalAmountQualifier](attributes)
    case ItemsAny         ⇒ Xor.Right(ItemsAnyQualifier)
    case _                ⇒ Xor.Left(QualifierNotImplementedFailure(qualifierType).single)
  }

  private def extract[T <: Qualifier](json: JValue)(implicit m: Manifest[T]): Xor[Failures, Qualifier] = {
    json.extractOpt[T] match {
      case Some(q) ⇒ Xor.Right(q)
      case None    ⇒ Xor.Left(QualifierAttributesExtractionFailure(qualifierType).single)
    }
  }
}
