package models

import util.TestBase
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.joda.time.DateTime

class CreditCardTest extends TestBase {
  val today = DateTime.now()
  val card = CreditCard(
    customerId = 1,
    gatewayCustomerId = "abcdef",
    lastFour = "4242",
    expMonth = today.getMonthOfYear,
    expYear  = today.getYear)

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

    "is inactive when deleted" in {
      card.copy(deletedAt = Some(DateTime.now)).isActive must be (false)
    }

    "is active when not deleted" in {
      card.copy(deletedAt = None).isActive must be (true)
    }
  }
}
