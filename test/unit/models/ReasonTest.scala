package models

import cats.data.NonEmptyList
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import services._
import util.TestBase
import utils.Seeds.Factories

class ReasonTest extends TestBase {
  "Reason" - {
    ".validateNew" - {
      "returns errors when body is empty" in {
        val emptyBodyReason = Factories.reason.copy(body = "")

        val reasons = Table(
          ("reason", "errors"),
          (emptyBodyReason, NonEmptyList(GeneralFailure("body must not be empty")))
        )

        forAll(reasons) { (reason: Reason, errors: NonEmptyList[GeneralFailure]) =>
          invalidValue(reason.validate) mustBe (errors)
        }
      }
    }
  }
}
