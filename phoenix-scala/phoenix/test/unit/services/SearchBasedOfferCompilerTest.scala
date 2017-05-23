package services

import org.json4s._
import org.json4s.jackson.JsonMethods.parse
import phoenix.failures.DiscountCompilerFailures.OfferSearchIsEmpty
import phoenix.models.discount.offers.ItemsPercentOff
import phoenix.services.discount.compilers.OfferCompiler
import testutils._

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
