package models

import cats.data.NonEmptyList
import failures.{Failure, GeneralFailure}
import models.location.{Address, Region}
import org.scalatest.prop.TableDrivenPropertyChecks._
import testutils.CustomMatchers._
import testutils.TestBase

class AddressTest extends TestBase {

  "Address" - {
    ".validateNew" - {
      val valid = Address(id = 0,
                          accountId = 1,
                          regionId = 1,
                          name = "Yax Fuentes",
                          address1 = "555 E Lake Union St.",
                          address2 = None,
                          city = "Seattle",
                          zip = "12345",
                          phoneNumber = None)

      def zipFailure(pattern: String): NonEmptyList[Failure] =
        NonEmptyList.of(buildMatchesFailure("zip", pattern))

      "returns errors when zip is invalid" in {
        val badZip         = valid.copy(zip = "AB+123")
        val wrongLengthZip = valid.copy(zip = "1")

        val addresses = Table(
            ("address", "errors"),
            (badZip, zipFailure(Address.zipPattern)),
            (wrongLengthZip, zipFailure(Address.zipPattern))
        )

        forAll(addresses) { (address, errors) ⇒
          invalidValue(address.validate) mustBe (errors)
        }
      }

      "return errors when US address and zip is not 5 or 9 digits" in {
        val tooShortZip    = valid.copy(zip = "1234")
        val wrongLengthZip = valid.copy(zip = "123456")

        val addresses = Table(
            ("address", "errors"),
            (tooShortZip, zipFailure(Address.zipPatternUs)),
            (wrongLengthZip, zipFailure(Address.zipPatternUs))
        )

        forAll(addresses) { (address, errors) ⇒
          invalidValue(address.copy(regionId = Region.usRegions.head).validate) mustBe (errors)
        }
      }

      "returns errors when name or address1 is empty" in {
        val result = valid.copy(name = "", address1 = "").validate
        invalidValue(result) must === (
            NonEmptyList.of[Failure](GeneralFailure("name must not be empty"),
                                     GeneralFailure("address1 must not be empty")))
      }

      "returns errors if US address and Some(phoneNumber) < 10 digits" in {
        val result =
          valid.copy(regionId = Region.usRegions.head, phoneNumber = Some("5551234")).validate
        invalidValue(result) must includeFailure(
            "phoneNumber must fully match regular expression '[0-9]{10}'")
      }

      "returns errors if non-US address and Some(phoneNumber) > 15 digits" in {
        val result = valid.copy(regionId = 1, phoneNumber = Some("1" * 16)).validate
        invalidValue(result) must includeFailure(
            "phoneNumber must fully match regular expression '[0-9]{0,15}'")
      }
    }
  }
}
