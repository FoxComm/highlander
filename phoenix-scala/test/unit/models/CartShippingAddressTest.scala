package models

import cats.data.NonEmptyList
import failures.{Failure, GeneralFailure}
import models.location.Address
import org.scalatest.prop.TableDrivenPropertyChecks._
import testutils.TestBase
import utils.seeds.Seeds.Factories

class CartShippingAddressTest extends TestBase {

  "OrderShippingAddress" - {
    ".validateNew" - {
      def zipFailure(pattern: String): NonEmptyList[Failure] =
        NonEmptyList.of(GeneralFailure(s"zip must fully match regular expression '$pattern'"))

      "returns errors when zip is invalid" in {
        val badZip         = Factories.shippingAddress.copy(regionId = 1, zip = "AB+123")
        val wrongLengthZip = Factories.shippingAddress.copy(regionId = 1, zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, zipFailure(Address.zipPattern)),
          (wrongLengthZip, zipFailure(Address.zipPattern))
        )

        forAll(addresses) { (address, errors) ⇒
          invalidValue(address.validate) mustBe (errors)
        }
      }
    }
  }
}
