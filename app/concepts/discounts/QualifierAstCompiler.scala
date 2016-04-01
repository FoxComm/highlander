package concepts.discounts

import cats.data.Xor
import concepts.discounts.qualifiers.Qualifier.QualifierAstFormat
import qualifiers._
import failures._
import failures.DiscountCompilerFailures._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.JsonFormatters

final case class QualifierAstCompiler(input: String) {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def compile(): Xor[Failures, Qualifier] = parseOpt(input) match {
    case Some(json) ⇒ compileInner(json)
    case _          ⇒ Xor.Left(QualifierAstParseFailure(input).single)
  }

  private def compileInner(json: JValue): Xor[Failures, Qualifier] = json.extractOpt[QualifierAstFormat] match {
    // Extract first element, currently
    case Some(q) ⇒ q.headOption match {
      case Some((qualifierType, attributes)) ⇒ QualifierCompiler(qualifierType, compact(attributes)).compile()
      case _                                 ⇒ Xor.Left(QualifierAstEmptyObjectFailure.single)
    }
    case _       ⇒ Xor.Left(QualifierAstInvalidFormatFailure.single)
  }
}
