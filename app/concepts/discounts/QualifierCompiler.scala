package concepts.discounts

import qualifiers._
import cats.data.Xor
import failures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

object QualifierCompiler {

  implicit val formats: Formats = JsonFormatters.DefaultFormats

  val dummyFailure = Xor.Left(GeneralFailure("I'm Dummy Failure!"))

  def compile(qualifierType: String, attributes: String): Xor[Failure, Qualifier] = {
    (QualifierType.read(qualifierType), parseOpt(attributes)) match {
      case (Some(q), Some(j)) ⇒ compileInner(q, j)
      case (Some(q), _)       ⇒ dummyFailure
      case (_, Some(j))       ⇒ dummyFailure
      case (_, _)             ⇒ dummyFailure
    }
  }

  private def compileInner(qualifierType: QualifierType, json: JValue): Xor[Failure, Qualifier] = qualifierType match {
    case OrderAny         ⇒ Xor.Right(OrderAnyQualifier)
    case OrderTotalAmount ⇒ extract[OrderTotalAmountQualifier](json)
    case ItemsAny         ⇒ Xor.Right(ItemsAnyQualifier)
    case _                ⇒ dummyFailure
  }

  private def extract[T <: Qualifier](json: JValue)(implicit m: Manifest[T]): Xor[Failure, Qualifier] = json.extractOpt[T] match {
    case Some(q) => Xor.Right(q)
    case None    => dummyFailure
  }
}