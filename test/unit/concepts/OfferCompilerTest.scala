package concepts

import concepts.discounts._
import concepts.discounts.OfferType.{show ⇒ show}
import concepts.discounts.offers._
import failures.DiscountCompilerFailures._
import util.TestBase

class OfferCompilerTest extends TestBase {

  "OfferCompiler" - {

    "succeeds for case object with valid, but empty attributes" in new CaseObjectFixture {
      rightValue(compiler.compile()) must === (FreeShippingOffer)
    }

    "succeeds for case class with valid attributes" in new CaseClassFixture {
      rightValue(compiler.compile()) must === (OrderPercentOffer(discount = 10))
    }

    "fails when incorrect type in attributes is passed" in new CaseClassInvalidTypeFixture {
      leftValue(compiler.compile()) must === (OfferAttributesExtractionFailure(offerType, attributes))
    }

    "returns failure for unknown qualifier type" in new UnknownQualifierFixture {
      leftValue(compiler.compile()) must === (UnknownOfferFailure(offerType))
    }

    "returns failure for invalid json attributes" in new InvalidJsonFixture {
      leftValue(compiler.compile()) must === (OfferAttributesParseFailure(offerType, attributes))
    }

    "returns failure when attributes are not matched to case class" in new UnmatchedCaseClassFixture {
      leftValue(compiler.compile()) must === (OfferAttributesExtractionFailure(offerType, attributes))
    }

    "returns failure when qualifier is not yet implemented" in new NotImplementedQualifierFixture {
      leftValue(compiler.compile()) must === (OfferNotImplementedFailure(offerType))
    }
  }

  val dummyAttributes = """{}"""

  def getTuple(offerType: String, attributes: String): (String, String, OfferCompiler) =
    (offerType, attributes, OfferCompiler(offerType, attributes))

  trait CaseObjectFixture {
    val (offerType, attributes, compiler) = getTuple(show(FreeShipping), dummyAttributes)
  }

  trait CaseClassFixture {
    val (offerType, attributes, compiler) = getTuple(show(OrderPercentOff), """{"discount": 10}""")
  }

  trait CaseClassInvalidTypeFixture {
    val (offerType, attributes, compiler) = getTuple(show(OrderPercentOff), """{"discount": false}""")
  }

  trait UnknownQualifierFixture {
    val (offerType, attributes, compiler) = getTuple("yolo", dummyAttributes)
  }

  trait InvalidJsonFixture {
    val (offerType, attributes, compiler) = getTuple(show(FreeShipping), """}{}{}{}{""")
  }

  trait UnmatchedCaseClassFixture {
    val (offerType, attributes, compiler) = getTuple(show(OrderPercentOff), dummyAttributes)
  }

  trait NotImplementedQualifierFixture {
    val (offerType, attributes, compiler) = getTuple(show(DiscountedShipping), dummyAttributes)
  }
}
