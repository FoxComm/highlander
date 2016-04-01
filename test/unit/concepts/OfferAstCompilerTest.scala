package concepts

import concepts.discounts._
import concepts.discounts.offers._
import failures.DiscountCompilerFailures._
import util.TestBase

class OfferAstCompilerTest extends TestBase {

  "OfferAstCompiler" - {

    "succeeds for case object with valid, but empty attributes" in new FreeShippingValidFixture {
      rightValue(compiler.compile()) must === (FreeShippingOffer)
    }

    "succeeds for case class with valid attributes" in new OrderPercentOfferValidFixture {
      rightValue(compiler.compile()) must === (OrderPercentOffer(discount = 30))
    }

    "fails when typo in configuration found" in new OrderTotalAmountTypoFixture {
      leftValue(compiler.compile()) must === (OfferAttributesExtractionFailure(typeName, attributes).single)
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
    val (json, compiler) = getTuple("""{"freeShipping": {}}""")
  }

  trait OrderPercentOfferValidFixture {
    val (json, compiler) = getTuple("""{"orderPercentOff": {"discount": 30}}""")
  }

  trait OrderTotalAmountTypoFixture {
    val typeName          = "orderPercentOff"
    val attributes        = """{"discounts":33}""" // compact()
    val (json, compiler)  = getTuple(s"""{"$typeName": $attributes}""")
  }

  trait InvalidJsonFixture {
    val (json, compiler) = getTuple("""""")
  }

  trait InvalidJsonFormatFixture {
    val (json, compiler) = getTuple("""{"freeShipping": [1, 2, 3]}""")
  }
}
