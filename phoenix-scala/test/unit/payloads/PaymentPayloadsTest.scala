package payloads

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import cats.implicits._
import failures.GeneralFailure
import org.scalatest.prop.TableDrivenPropertyChecks._
import payloads.PaymentPayloads._
import testutils.CustomMatchers._
import testutils.TestBase
import utils.seeds.Seeds.Factories

class PaymentPayloadsTest extends TestBase {
  val today = ZonedDateTime.now()
  val cc    = Factories.creditCard

  "CreateCreditCard" - {
    val valid = CreateCreditCardFromSourcePayload(holderName = cc.holderName,
                                                  cardNumber = "4242424242424242",
                                                  cvv = "123",
                                                  expYear = cc.expYear,
                                                  expMonth = cc.expMonth,
                                                  addressId = Some(1))

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
    val valid = EditCreditCard(holderName = cc.holderName.some,
                               expYear = cc.expYear.some,
                               expMonth = cc.expMonth.some,
                               addressId = 1.some)

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
        val expired = valid.copy(expMonth = today.getMonthValue.some,
                                 expYear = today.minusYears(1).getYear.some)

        val cards = Table(
            ("payload", "errors"),
            (expired, NonEmptyList.of(GeneralFailure("credit card is expired"))),
            (expired.copy(expYear = 2000.some),
             NonEmptyList.of(GeneralFailure("credit card is expired")))
        )

        forAll(cards) { (card, errors) ⇒
          invalidValue(card.validate) mustBe errors
        }
      }

      "passes when valid" in {
        valid.validate mustBe 'valid
      }
    }
  }
}
