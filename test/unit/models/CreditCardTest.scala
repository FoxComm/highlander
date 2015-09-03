package models

import cats.data._
import util.TestBase
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.joda.time.DateTime
import utils.Seeds.Factories

class CreditCardTest extends TestBase {
  val today = DateTime.now()
  val card = Factories.creditCard

  "CreditCard" - {
    "validateNew" - {
      "disallows cards with expired dates" in {
        val expiredCard = card.copy(expMonth = today.minusMonths(1).getMonthOfYear, expYear = today.getYear)

        val cards = Table(
          ("card", "errors"),
          (expiredCard, NonEmptyList("credit card is expired")),
          (card.copy(expYear = 2000), NonEmptyList("credit card is expired"))
        )

        forAll(cards) { (card: CreditCard, errors: NonEmptyList[String]) =>
          invalidValue(card.validateNew) must === (errors)
        }
      }

      "disallows cards with dates past the singularity (> 20 years from today)" in {
        val result = card.copy(expYear = card.expYear + 21).validateNew
        invalidValue(result).head must include("credit card expiration is too far in the future")
      }

      "passes for valid cards" in {
        val result = card.validateNew
        result.isValid mustBe true
      }
    }
  }
}
