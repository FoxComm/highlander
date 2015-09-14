package payloads

import java.time.ZonedDateTime

import services.{Failure, GeneralFailure}
import util.TestBase
import org.scalatest.prop.TableDrivenPropertyChecks._
import utils.Seeds.Factories
import util.CustomMatchers._
import cats.data.NonEmptyList
import cats.implicits._

class PaymentPayloadsTest extends TestBase {
  val today = ZonedDateTime.now()
  val cc = Factories.creditCard

  "CreateCreditCard" - {
    val valid = payloads.CreateCreditCard(holderName = cc.holderName, number = "4242424242424242",
      cvv = "123", expYear = cc.expYear, expMonth = cc.expMonth, addressId = Some(1))

    "validate" - {
      "fails if neither addressId nor address are provided" in {
        valid.copy(addressId = None, address = None).validate mustBe 'invalid
      }

      "passes when valid" in {
        valid.validate mustBe 'valid
      }
    }
  }

  "EditCreditCard" - {
    val valid = payloads.EditCreditCard(holderName = cc.holderName.some, expYear = cc.expYear.some,
      expMonth = cc.expMonth.some, addressId = 1.some)

    "validate" - {
      "fails if holderName is an empty string" in {
        val res = valid.copy(holderName = "".some).validate

        res mustBe 'invalid
        invalidValue(res) must includeFailure("holderName must not be empty")
      }

      "fails if month is not a month of the year" in {
        val res = valid.copy(expMonth = 0.some).validate

        res mustBe 'invalid
        invalidValue(res) must includeFailure("expiration month got 0, expected between 1 and 12")
      }

      "fails with expired date" in {
        val expired = valid.copy(expMonth = today.minusMonths(1).getMonthValue.some, expYear = today.getYear.some)

        val cards = Table(
          ("payload", "errors"),
          (expired, NonEmptyList(GeneralFailure("credit card is expired"))),
          (expired.copy(expYear = 2000.some), NonEmptyList(GeneralFailure("credit card is expired")))
        )

        forAll(cards) { (card, errors) =>
          invalidValue(card.validate) mustBe errors
        }
      }

      "passes when valid" in {
        valid.validate mustBe 'valid
      }
    }
  }
}
