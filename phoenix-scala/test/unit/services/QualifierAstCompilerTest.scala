package services

import failures.DiscountCompilerFailures._
import models.discount.qualifiers._
import org.json4s.jackson.JsonMethods._
import services.discount.compilers.QualifierAstCompiler
import testutils.TestBase

class QualifierAstCompilerTest extends TestBase {

  "QualifierAstCompiler" - {

    "succeeds for case object with valid, but empty attributes" in new OrderAnyValidFixture {
      rightValue(compiler.compile()) must === (AndQualifier(Seq(OrderAnyQualifier)))
    }

    "succeeds for case class with valid attributes" in new OrderTotalAmountValidFixture {
      rightValue(compiler.compile()) must === (
        AndQualifier(Seq(OrderTotalAmountQualifier(totalAmount = 1))))
    }

    "fails when typo in configuration found" in new OrderTotalAmountTypoFixture {
      leftValue(compiler.compile()) must === (
        QualifierAttributesExtractionFailure(OrderTotalAmount).single)
    }
  }

  def getTuple(json: String): (String, QualifierAstCompiler) =
    (json, QualifierAstCompiler(parse(json)))

  trait OrderAnyValidFixture {
    val typeName         = QualifierType.show(OrderAny)
    val (json, compiler) = getTuple(s"""{"$typeName": {}}""")
  }

  trait OrderTotalAmountValidFixture {
    val typeName         = QualifierType.show(OrderTotalAmount)
    val (json, compiler) = getTuple(s"""{"$typeName": {"totalAmount": 1}}""")
  }

  trait OrderTotalAmountTypoFixture {
    val typeName         = QualifierType.show(OrderTotalAmount)
    val (json, compiler) = getTuple(s"""{"$typeName": {"totalAmounts": 1}}""")
  }
}
