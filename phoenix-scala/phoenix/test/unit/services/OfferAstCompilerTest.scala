package services

import org.json4s.jackson.JsonMethods._
import phoenix.failures.DiscountCompilerFailures._
import phoenix.models.discount.offers._
import phoenix.services.discount.compilers.OfferAstCompiler
import testutils.TestBase

class OfferAstCompilerTest extends TestBase {

  "OfferAstCompiler" - {

    "succeeds for case object with valid, but empty attributes" in new FreeShippingValidFixture {
      rightValue(compiler.compile()) must === (OfferList(List(FreeShippingOffer)))
    }

    "succeeds for case class with valid attributes" in new OrderPercentOfferValidFixture {
      rightValue(compiler.compile()) must === (OfferList(List(OrderPercentOffer(discount = 30))))
    }

    "fails when typo in configuration found" in new OrderPercentOfferTypoFixture {
      val cause =
        "No usable value for discount\nDid not find value which can be converted into long"
      leftValue(compiler.compile()) must === (OfferAttributesExtractionFailure(OrderPercentOff, cause).single)
    }
  }

  def getTuple(json: String): (String, OfferAstCompiler) =
    (json, OfferAstCompiler(parse(json)))

  trait FreeShippingValidFixture {
    val typeName         = OfferType.show(FreeShipping)
    val (json, compiler) = getTuple(s"""{"$typeName": {}}""")
  }

  trait OrderPercentOfferValidFixture {
    val typeName         = OfferType.show(OrderPercentOff)
    val (json, compiler) = getTuple(s"""{"$typeName": {"discount": 30}}""")
  }

  trait OrderPercentOfferTypoFixture {
    val typeName         = OfferType.show(OrderPercentOff)
    val (json, compiler) = getTuple(s"""{"$typeName": {"discounts": 30}}""")
  }
}
