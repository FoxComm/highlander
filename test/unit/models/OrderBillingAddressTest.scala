package models

import cats.data.NonEmptyList
import org.scalatest.prop.TableDrivenPropertyChecks._
import utils.Seeds.Factories
import util.TestBase
import utils.Slick.implicits._

class OrderBillingAddressTest extends TestBase {
  "OrderBillingAddress" - {
    ".validateNew" - {
      "returns errors when zip is invalid" in {
        val badZip = Factories.billingAddress.copy(regionId = 1, zip = "AB+123")
        val wrongLengthZip = Factories.billingAddress.copy(regionId = 1, zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, NonEmptyList("zip must fully match regular expression '%s'".format(Address.zipPattern))),
          (wrongLengthZip, NonEmptyList("zip must fully match regular expression '%s'".format(Address.zipPattern)))
        )

        forAll(addresses) { (address: OrderBillingAddress, errors: NonEmptyList[String]) =>
          invalidValue(address.validateNew) must === (errors)
        }
      }
    }
  }
}
