package models

import java.time.ZonedDateTime

import cats.data._
import failures.{Failure, GeneralFailure}
import models.location.Address
import org.scalatest.prop.TableDrivenPropertyChecks._
import testutils.CustomMatchers._
import testutils.TestBase
import utils.seeds.Seeds.Factories

class CreditCardTest extends TestBase {
  val today = ZonedDateTime.now()
  val card  = Factories.creditCard

  "CreditCard" - {
    "validateNew" - {
      "disallows cards with expired dates" in {
        val expiredCard =
          card.copy(expMonth = today.getMonthValue, expYear = today.minusYears(1).getYear)

        val cards = Table(
            ("card", "errors"),
            (expiredCard, NonEmptyList.of(GeneralFailure("credit card is expired"))),
            (card.copy(expYear = 2000), NonEmptyList.of(GeneralFailure("credit card is expired")))
        )

        forAll(cards) { (card, errors) ⇒
          invalidValue(card.validate) mustBe errors
        }
      }

      "disallows cards with dates past the singularity (> 20 years from today)" in {
        val result = card.copy(expYear = card.expYear + 21).validate
        invalidValue(result) must includeFailure("credit card expiration is too far in the future")
      }

      "passes for valid cards" in {
        val result = card.validate
        result.isValid mustBe true
      }

      "returns errors when zip is invalid" in {
        val zipFailure: NonEmptyList[Failure] = NonEmptyList.of(
            GeneralFailure(s"zip must fully match regular expression '${Address.zipPatternUs}'"))

        val badZip         = card.copy(zip = "AB+123")
        val wrongLengthZip = card.copy(zip = "1")

        val cards = Table(
            ("creditCards", "errors"),
            (badZip, zipFailure),
            (wrongLengthZip, zipFailure)
        )

        forAll(cards) { (cc, errors) ⇒
          invalidValue(cc.validate) mustBe errors
        }
      }
    }
  }
}
