package models

import cats.data.NonEmptyList
import services._
import org.scalatest.prop.TableDrivenPropertyChecks._
import util.TestBase
import util.CustomMatchers._

class AddressTest extends TestBase {

  "Address" - {
    ".validateNew" - {
      val valid = Address(id = 0, customerId = 1, regionId = 1, name = "Yax Home",
        street1 = "555 E Lake Union St.", street2 = None, city = "Seattle", zip = "12345", phoneNumber = None)

      def zipFailure(pattern: String): NonEmptyList[Failure] =
        NonEmptyList(GeneralFailure(s"zip must fully match regular expression $pattern"))

      "returns errors when zip is invalid" in {
        val badZip = valid.copy(zip = "AB+123")
        val wrongLengthZip = valid.copy(zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, zipFailure(Address.zipPattern)),
          (wrongLengthZip, zipFailure(Address.zipPattern))
        )

        forAll(addresses) { (address, errors) =>
          invalidValue(address.validateNew) mustBe (errors)
        }
      }

      "return errors when US address and zip is not 5 or 9 digits" in {
        val tooShortZip = valid.copy(zip = "1234")
        val wrongLengthZip = valid.copy(zip = "123456")

        val addresses = Table(
          ("address", "errors"),
          (tooShortZip, zipFailure(Address.zipPatternUs)),
          (wrongLengthZip, zipFailure(Address.zipPatternUs))
        )

        forAll(addresses) { (address, errors) =>
          invalidValue(address.copy(regionId = Country.usRegions.head).validateNew) mustBe (errors)
        }
      }

      "returns errors when name or street1 is empty" in {
        val result = valid.copy(name = "", street1 = "").validateNew
        invalidValue(result) must === (NonEmptyList[Failure](GeneralFailure("name must not be empty"), GeneralFailure("street1 must " +
          "not be empty")))
      }

      "returns errors if US address and Some(phoneNumber) < 10 digits" in {
        val result = valid.copy(regionId = Country.usRegions.head, phoneNumber = Some("5551234")).validateNew
        invalidValue(result) must (includeFailure("phoneNumber") and includeFailure("'[0-9]{10}'"))
      }

      "returns errors if non-US address and Some(phoneNumber) > 15 digits" in {
        val result = valid.copy(regionId = 1, phoneNumber = Some("1" * 16)).validateNew
        invalidValue(result) must (includeFailure("phoneNumber") and includeFailure("'[0-9]{0,15}'"))
      }
    }
  }
}
