package services

import failures.DiscountCompilerFailures.OfferSearchIsEmpty
import failures._
import models.discount.offers.ItemsPercentOff
import services.discount.compilers.OfferCompiler
import testutils._
import utils.json.yolo._

class SearchBasedOfferCompilerTest extends TestBase {

  "search-based offer" - {

    "must compile" in {
      val json     = """
          |{
          |  "discount": 1,
          |  "search": [{"productSearchId": 1}]
          |}
        """.stripMargin
      val compiler = OfferCompiler(ItemsPercentOff, parse(json))
      compiler.compile() mustBe 'right
    }

    "must not allow empty search" in {
      val json     = """
          |{
          |  "discount": 1,
          |  "search": []
          |}
        """.stripMargin
      val compiler = OfferCompiler(ItemsPercentOff, parse(json))
      compiler.compile().leftVal must === (OfferSearchIsEmpty(ItemsPercentOff).single)
    }
  }
}
