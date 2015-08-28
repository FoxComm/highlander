package models

import org.scalatest.prop.TableDrivenPropertyChecks._
import utils.Seeds.Factories
import util.TestBase

class OrderShippingAddressTest extends TestBase {

  "OrderShippingAddress" - {
    ".validate" - {
      "returns errors when zip is invalid" in {
        val badZip = Factories.shippingAddress.copy(zip = "AB+123")
        val wrongLengthZip = Factories.shippingAddress.copy(zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, Set("zip must match regular expression '%s'".format(Address.zipCodePattern))),
          (wrongLengthZip, Set("zip must match regular expression '%s'".format(Address.zipCodePattern)))
        )

        forAll(addresses) { case (address, errors) =>
          address.validate.messages must === (errors)
        }
      }
    }
  }
}
