package concepts

import concepts.discounts._
import concepts.discounts.offers._
import failures.DiscountCompilerFailures._
import util.TestBase

class OfferAstCompilerTest extends TestBase {

  "OfferAstCompiler" - {

    "succeeds for case object with valid, but empty attributes" in new FreeShippingValidFixture {
      rightValue(compiler.compile()) must === (OfferList(List(FreeShippingOffer)))
    }

    "succeeds for case class with valid attributes" in new OrderPercentOfferValidFixture {
      rightValue(compiler.compile()) must === (OfferList(List(OrderPercentOffer(discount = 30))))
    }

    "fails when typo in configuration found" in new OrderPercentOfferTypoFixture {
      leftValue(compiler.compile()) must === (OfferAttributesExtractionFailure(OrderPercentOff).single)
    }

    "fails when invalid json provided" in new InvalidJsonFixture {
      leftValue(compiler.compile()) must === (OfferAstParseFailure(json).single)
    }

    "fails when invalid json format provided" in new InvalidJsonFormatFixture {
      leftValue(compiler.compile()) must === (OfferAstInvalidFormatFailure.single)
    }
  }

  def getTuple(json: String): (String, OfferAstCompiler) =
    (json, OfferAstCompiler(json))

  trait FreeShippingValidFixture {
    val typeName         = OfferType.show(FreeShipping)
    val (json, compiler) = getTuple(s"""{"$typeName": {}}""")
  }

  trait OrderPercentOfferValidFixture {
    val typeName         = OfferType.show(OrderPercentOff)
    val (json, compiler) = getTuple(s"""{"$typeName": {"discount": 30}}""")
  }

  trait OrderPercentOfferTypoFixture {
    val typeName          = OfferType.show(OrderPercentOff)
    val (json, compiler)  = getTuple(s"""{"$typeName": {"discounts": 30}}""")
  }

  trait InvalidJsonFixture {
    val (json, compiler) = getTuple("""""")
  }

  trait InvalidJsonFormatFixture {
    val (json, compiler) = getTuple("""[1, 2, 3]""")
  }
}
