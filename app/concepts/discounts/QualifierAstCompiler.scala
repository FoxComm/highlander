package concepts.discounts

import cats.data.Xor
import qualifiers._
import failures._
import failures.DiscountCompilerFailures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

final case class QualifierAstCompiler(input: String) {

  type QualifierAstFormat = Map[String, Map[String, JValue]]

  implicit val formats: Formats = JsonFormatters.DefaultFormats

  def compile(): Xor[Failures, Qualifier] = parseOpt(input) match {
    case Some(json) ⇒ compileInner(json)
    case _          ⇒ Xor.Left(QualifierAstParseFailure(input).single)
  }

  // Extract first element, currently
  private def compileInner(json: JValue): Xor[Failures, Qualifier] = json.extractOpt[QualifierAstFormat] match {
    case Some(q) ⇒ q.headOption match {
      case Some((qualifierType, attributes)) ⇒ Xor.Right(OrderAnyQualifier) // FIXME
      case _                                 ⇒ Xor.Left(QualifierAstEmptyObjectFailure.single)
    }
    case _       ⇒ Xor.Left(QualifierAstInvalidFormatFailure.single)
  }
}
