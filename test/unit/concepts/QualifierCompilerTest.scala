package concepts

import concepts.discounts._
import concepts.discounts.QualifierType.{show ⇒ show}
import concepts.discounts.qualifiers._
import failures.DiscountCompilerFailures._
import util.TestBase

class QualifierCompilerTest extends TestBase {

  "QualifierCompiler" - {

    "succeeds for case object with valid, but empty attributes" in new CaseObjectFixture {
      rightValue(compiler.compile()) must === (OrderAnyQualifier)
    }

    "succeeds for case class with valid attributes" in new CaseClassFixture {
      rightValue(compiler.compile()) must === (OrderTotalAmountQualifier(totalAmount = 1))
    }

    "fails when incorrect type in attributes is passed" in new CaseClassInvalidTypeFixture {
      leftValue(compiler.compile()) must === (QualifierAttributesExtractionFailure(qualifierType, attributes))
    }

    "returns failure for unknown qualifier type" in new UnknownQualifierFixture {
      leftValue(compiler.compile()) must === (UnknownQualifierFailure(qualifierType))
    }

    "returns failure for invalid json attributes" in new InvalidJsonFixture {
      leftValue(compiler.compile()) must === (QualifierAttributesParseFailure(qualifierType, attributes))
    }

    "returns failure when attributes are not matched to case class" in new UnmatchedCaseClassFixture {
      leftValue(compiler.compile()) must === (QualifierAttributesExtractionFailure(qualifierType, attributes))
    }

    "returns failure when qualifier is not yet implemented" in new NotImplementedQualifierFixture {
      leftValue(compiler.compile()) must === (QualifierNotImplementedFailure(qualifierType))
    }
  }

  val dummyAttributes = """{}"""

  def getTuple(qualifierType: String, attributes: String): (String, String, QualifierCompiler) =
    (qualifierType, attributes, QualifierCompiler(qualifierType, attributes))

  trait CaseObjectFixture {
    val (qualifierType, attributes, compiler) = getTuple(show(OrderAny), dummyAttributes)
  }

  trait CaseClassFixture {
    val (qualifierType, attributes, compiler) = getTuple(show(OrderTotalAmount), """{"totalAmount": 1}""")
  }

  trait CaseClassInvalidTypeFixture {
    val (qualifierType, attributes, compiler) = getTuple(show(OrderTotalAmount), """{"totalAmount": false}""")
  }

  trait UnknownQualifierFixture {
    val (qualifierType, attributes, compiler) = getTuple("yolo", dummyAttributes)
  }

  trait InvalidJsonFixture {
    val (qualifierType, attributes, compiler) = getTuple(show(OrderAny), """}{}{}{}{""")
  }

  trait UnmatchedCaseClassFixture {
    val (qualifierType, attributes, compiler) = getTuple(show(OrderTotalAmount), dummyAttributes)
  }

  trait NotImplementedQualifierFixture {
    val (qualifierType, attributes, compiler) = getTuple(show(OrderNumUnits), dummyAttributes)
  }
}
