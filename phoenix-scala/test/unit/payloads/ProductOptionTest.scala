package payloads

import cats.implicits._
import failures.GeneralFailure
import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.PropertyChecks
import payloads.ProductOptionPayloads.{ProductOptionPayload, ProductOptionValuePayload}
import scala.util.Random
import testutils.TestBase
import utils.FoxValidationException

class ProductOptionTest extends TestBase with PropertyChecks {
  "ProductOptionPayload" - {
    "creation" - {
      "fails when multiple options are attached to the same variant" in {
        val skuCodesGen = for {
          invalid    ← Gen.nonEmptyContainerOf[Set, String](Gen.alphaStr)
          times      ← Gen.choose(2, 10)
          repeated   ← Gen.listOfN(times, Gen.const(invalid))
          validFront ← Gen.containerOf[Set, String](Gen.alphaStr)
          validBack ← Gen.containerOf[Set, String](Gen.alphaStr.suchThat(c ⇒
                               !validFront.contains(c)))
        } yield
          (validFront.toList ++ Random.shuffle(repeated.flatten) ++ validBack.toList, invalid)
        forAll(skuCodesGen) {
          case (all, repeated) ⇒
            val expected = repeated.map(code ⇒
                  GeneralFailure(s"Variant $code cannot have more than one option value"))
            intercept[FoxValidationException] {
              ProductOptionPayload(attributes = Map.empty,
                                   values = List(
                                       ProductOptionValuePayload(
                                           name = Random.nextString(10).some,
                                           swatch = None,
                                           skuCodes = all
                                       )).some)
            }.failures.toList must contain theSameElementsAs expected
        }
      }

      "passes when valid" in {
        val skuCodesGen = Gen.containerOf[Set, String](Gen.alphaStr)
        forAll(skuCodesGen) { skuCodes ⇒
          ProductOptionPayload(attributes = Map.empty,
                               values = List(
                                   ProductOptionValuePayload(
                                       name = Random.nextString(10).some,
                                       swatch = None,
                                       skuCodes = skuCodes.toList
                                   )).some).validate.isValid must === (true)
        }
      }
    }
  }
}
