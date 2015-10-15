package models

import org.scalatest.prop.TableDrivenPropertyChecks._
import util.TestBase
import util.CustomMatchers._
import utils.Seeds.Factories

class CustomerTest extends TestBase {
  "Customer" - {
    ".validate" - {
      "when isGuest" - {
        "fails if email is blank" in {
          val customer = Factories.customer.copy(email = "")
          val result = customer.validate

          result mustBe 'invalid
          invalidValue(result) must includeFailure("email must not be empty")
        }

        "succeeds" in {
          Factories.customer.validate mustBe 'valid
        }
      }

      "when NOT isGuest" - {
        "fails" in {
          val c = Factories.customer

          val customers = Table(
            ("customer", "errors"),
            (c.copy(email = ""), "email must not be empty"),
            (c.copy(name = None), "name must not be empty"),
            (c.copy(name = Some("")), "name must not be empty")
          )

          forAll(customers) { case (customer, errors) ⇒
            val result = customer.validate
            result mustBe 'invalid
            invalidValue(result) must includeFailure(errors)
          }
        }

        "succeeds" in {
          Factories.customer.validate mustBe 'valid
        }
      }
    }
  }
}
