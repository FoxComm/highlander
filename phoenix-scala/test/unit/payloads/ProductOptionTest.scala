package payloads

import cats.implicits._
import failures.ProductFailures.DuplicatedOptionValueForVariant
import payloads.ProductOptionPayloads.{ProductOptionPayload, ProductOptionValuePayload}
import scala.util.Random
import testutils.TestBase
import utils.FoxValidationException

class ProductOptionTest extends TestBase {
  "ProductOptionPayload" - {
    "creation" - {
      "fails when multiple options are attached to the same variant" in {
        intercept[FoxValidationException] {
          ProductOptionPayload(attributes = Map.empty,
                               values = List(
                                   ProductOptionValuePayload(
                                       name = Random.nextString(10).some,
                                       swatch = None,
                                       skus = List("duplicated1",
                                                   "whatever1",
                                                   "duplicated1",
                                                   "duplicated2",
                                                   "duplicated2",
                                                   "whatever2")
                                   )).some)
        }.failures.toList must contain theSameElementsAs List(
            DuplicatedOptionValueForVariant("duplicated1"),
            DuplicatedOptionValueForVariant("duplicated2"))
      }

      "passes when valid" in {
        ProductOptionPayload(attributes = Map.empty,
                             values = List(
                                 ProductOptionValuePayload(
                                     name = Random.nextString(10).some,
                                     swatch = None,
                                     skus = List("whatever1", "whatever2", "whatever3")
                                 )).some).validate.isValid must === (true)
      }
    }
  }
}
