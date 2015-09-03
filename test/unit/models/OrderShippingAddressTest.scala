package models

import cats.data.NonEmptyList
import org.scalatest.prop.TableDrivenPropertyChecks._
import services._
import utils.Seeds.Factories
import util.TestBase

class OrderShippingAddressTest extends TestBase {

  "OrderShippingAddress" - {
    ".validateNew" - {
      "returns errors when zip is invalid" in {
        val badZip = Factories.shippingAddress.copy(regionId = 1, zip = "AB+123")
        val wrongLengthZip = Factories.shippingAddress.copy(regionId = 1, zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, NonEmptyList[Failure](GeneralFailure("zip must fully match regular expression '%s'".format(Address
            .zipPattern)))),
          (wrongLengthZip, NonEmptyList[Failure](GeneralFailure("zip must fully match regular expression '%s'".format
            (Address.zipPattern))))
        )

        forAll(addresses) { (address: OrderShippingAddress, errors: NonEmptyList[Failure]) =>
          invalidValue(address.validateNew) must === (errors)
        }
      }
    }
  }
}
