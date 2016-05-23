package payloads

import java.time.ZonedDateTime

import cats.implicits._
import payloads.OrderPayloads.CreateOrder
import util.CustomMatchers._
import util.TestBase
import utils.seeds.Seeds.Factories

class OrderPayloadsTest extends TestBase {
  val today = ZonedDateTime.now()
  val cc = Factories.creditCard

  "CreateOrder" - {
    val valid = CreateOrder(customerId = 1.some)

    "validate" - {
      "fails if neither customerId nor email are provided" in {
        val result = valid.copy(customerId = None).validate

        result mustBe 'invalid
        invalidValue(result) must includeFailure("customerId or email must be given")
      }

      "fails if email is blank" in {
        val result = valid.copy(customerId = None, email = "".some).validate

        result mustBe 'invalid
        invalidValue(result) must includeFailure("email must not be empty")
      }

      "succeeds" in {
        valid.validate mustBe 'valid
      }
    }
  }
}

