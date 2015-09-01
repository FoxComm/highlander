package models

import util.TestBase
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.joda.time.DateTime
import utils.Seeds.Factories

class CreditCardTest extends TestBase {
  val today = DateTime.now()
  val card = Factories.creditCard

  "CreditCardGateway" - {
    "validations" - {
      "disallows cards with expired dates" in {
        val cards = Table(
          ("card", "errors"),
          (card.copy(expMonth = today.minusMonths(1).getMonthOfYear), "credit card is expired"),
          (card.copy(expYear  = 2000), "credit card is expired")
        )

        forAll(cards) { (c, error) =>
          val result = c.validate

          result mustBe 'invalid

          withClue(result.messages) {
            result.messages.size must ===(1)
          }
          result.messages.head mustBe (error)
        }
      }

      "disallows cards with dates past the singularity (> 20 years from today)" in {
        val result = card.copy(expYear = card.expYear + 21).validate

        result mustBe 'invalid
        result.messages.size must === (1)
        result.messages.head must === ("credit card expiration is too far in the future")
      }

      "passes for valid cards" in {
        val result = card.validate
        result mustBe 'valid
        result.messages mustBe 'empty
      }
    }
  }
}
