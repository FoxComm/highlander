package models

import models.account.User
import org.scalatest.prop.TableDrivenPropertyChecks._
import testutils.CustomMatchers._
import testutils.TestBase
import utils.seeds.Seeds.Factories

class CustomerTest extends TestBase {
  "Customer" - {
    ".validate" - {
      "when isGuest" - {
        "fails if name contains '@' character" in {
          val customer = Factories.customer.copy(name = Some("hi@there"))
          val result   = customer.validate
          result mustBe 'invalid
          invalidValue(result) must includeMatchesFailure("name", User.namePattern)
        }

        "succeeds" in {
          Factories.customer.validate mustBe 'valid
        }
      }

      "when NOT isGuest" - {
        "fails" in {
          val c = Factories.customer

          val customers = Table(
            ("users", "errors"),
            (c.copy(email = Some("")), "email must not be empty"),
            (c.copy(name = Some("")), "name must not be empty")
          )

          forAll(customers) {
            case (customer, errors) ⇒
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
