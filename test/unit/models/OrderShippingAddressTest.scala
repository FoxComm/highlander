package models

import cats.data.NonEmptyList
import org.scalatest.prop.TableDrivenPropertyChecks._
import services._
import utils.Seeds.Factories
import util.TestBase

class OrderShippingAddressTest extends TestBase {

  "OrderShippingAddress" - {
    ".validateNew" - {
      def zipFailure(pattern: String): NonEmptyList[Failure] =
        NonEmptyList(GeneralFailure(s"zip must fully match regular expression $pattern"))

      "returns errors when zip is invalid" in {
        val badZip = Factories.shippingAddress.copy(regionId = 1, zip = "AB+123")
        val wrongLengthZip = Factories.shippingAddress.copy(regionId = 1, zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, zipFailure(Address.zipPattern)),
          (wrongLengthZip, zipFailure(Address.zipPattern))
        )

        forAll(addresses) { (address, errors) =>
          invalidValue(address.validateNew) mustBe (errors)
        }
      }
    }
  }
}
