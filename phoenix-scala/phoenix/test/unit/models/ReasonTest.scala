package models

import cats.data.NonEmptyList
import core.failures.GeneralFailure
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import phoenix.models.Reason
import phoenix.utils.seeds.Factories
import testutils.TestBase

class ReasonTest extends TestBase {
  "Reason" - {
    ".validateNew" - {
      "returns errors when body is empty" in {
        val emptyBodyReason = Factories.reason(0).copy(body = "")

        val reasons = Table(
          ("reason", "errors"),
          (emptyBodyReason, NonEmptyList.of(GeneralFailure("body must not be empty")))
        )

        forAll(reasons) { (reason: Reason, errors: NonEmptyList[GeneralFailure]) ⇒
          invalidValue(reason.validate) mustBe errors
        }
      }
    }
  }
}
