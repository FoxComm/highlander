package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import org.scalactic.{Bad, Good}
import org.scalatest.prop.TableDrivenPropertyChecks._
import payloads.CreateAddressPayload
import util.IntegrationTestBase
import utils.Validation
import utils.Seeds.Factories

class AddressTest extends IntegrationTestBase {
  import api._

  import concurrent.ExecutionContext.Implicits.global

  "Address" - {
    ".validate" - {
      "returns errors when zip is not 5 digit chars" in {
        val valid = Address(id = 0, customerId = 1, stateId = 1, name = "Yax Home",
          street1 = "555 E Lake Union St.", street2 = None, city = "Seattle", zip = "12345")

        val badZip = valid.copy(zip = "AB123")
        val wrongLengthZip = valid.copy(zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, Set("zip must match regular expression '[0-9]{5}'")),
          (wrongLengthZip, Set("zip must match regular expression '[0-9]{5}'"))
        )

        forAll(addresses) { (address: Address, errors: Set[String]) =>
          address.validate.messages must === (errors)
        }
      }
    }
  }

  trait CustomerFixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }
}
