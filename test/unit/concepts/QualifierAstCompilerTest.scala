package concepts

import concepts.discounts._
import concepts.discounts.QualifierType.{show ⇒ show}
import concepts.discounts.qualifiers._
import failures.DiscountCompilerFailures._
import util.TestBase

class QualifierAstCompilerTest extends TestBase {

  "QualifierAstCompiler" - {

    /*
    "succeeds for case object with valid, but empty attributes" in new CaseObjectFixture {
      rightValue(compiler.compile()) must === (OrderAnyQualifier)
    }

    "succeeds for case class with valid attributes" in new CaseClassFixture {
      rightValue(compiler.compile()) must === (OrderTotalAmountQualifier(totalAmount = 1))
    }
    */

    "fails when invalid json provided" in new InvalidJsonFixture {
      leftValue(compiler.compile()) must === (QualifierAstParseFailure(json).single)
    }

    "fails when invalid json format provided" in new InvalidJsonFormatFixture {
      leftValue(compiler.compile()) must === (QualifierAstInvalidFormatFailure.single)
    }
  }

  val dummyAttributes = """{}"""

  def getTuple(json: String): (String, QualifierAstCompiler) =
    (json, QualifierAstCompiler(json))

  trait InvalidJsonFixture {
    val (json, compiler) = getTuple("""""")
  }

  trait InvalidJsonFormatFixture {
    val (json, compiler) = getTuple("""{"orderAny": [1, 2, 3]}""")
  }
}
