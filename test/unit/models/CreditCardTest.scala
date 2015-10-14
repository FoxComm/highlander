package models

import java.time.ZonedDateTime

import cats.data._
import util.TestBase
import services._
import org.scalatest.prop.TableDrivenPropertyChecks._
import utils.Seeds.Factories
import util.CustomMatchers._

class CreditCardTest extends TestBase {
  val today = ZonedDateTime.now()
  val card = Factories.creditCard

  "CreditCard" - {
    "validateNew" - {
      "disallows cards with expired dates" in {
        val expiredCard = card.copy(expMonth = today.minusMonths(1).getMonthValue, expYear = today.getYear)

        val cards = Table(
          ("card", "errors"),
          (expiredCard, NonEmptyList(GeneralFailure("credit card is expired"))),
          (card.copy(expYear = 2000), NonEmptyList(GeneralFailure("credit card is expired")))
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
        val zipFailure: NonEmptyList[Failure] =
          NonEmptyList(GeneralFailure(s"zip must fully match regular expression '${Address.zipPatternUs}'"))

        val badZip = card.copy(zip = "AB+123")
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
