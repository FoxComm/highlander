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

  "Addresses" - {
    lazy val state = States.findByAbbrev("WA").futureValue.get

    "createFromPayload" - {
      "fails if address(es) do not pass validations" in new CustomerFixture {
        val payload = Seq(CreateAddressPayload(name = "Office", stateId = state.id, street1 = "3000 Burlingame Ave.",
          street2 = None, city = "Burlingame", zip = "NOPE"))

        Addresses.createFromPayload(customer, payload).futureValue match {
          case Good(_) =>
            fail("address should have failed validation")

          case Bad(errorMap) =>
            val (address, errors) = errorMap.head
            address.name must be("Office")
            errors must contain("zip must match regular expression '[0-9]{5}'")
        }
      }

      "creates address(es) successfully" in new CustomerFixture {
        val payload = Seq(CreateAddressPayload(name = "Office", stateId = state.id, street1 = "3000 Burlingame Ave.",
          street2 = None, city = "Burlingame", zip = "12345"))

        Addresses.createFromPayload(customer, payload).futureValue match {
          case Good(addresses) =>
            addresses.length must be(1)
            addresses.head.id must be > 0

          case Bad(errorMap) =>
            fail(errorMap.mkString(";") ++ "address should have passed validation")
        }
      }
    }
  }

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
