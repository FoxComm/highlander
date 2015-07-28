package models

import util.TestBase
import utils.Seeds.Factories

class ReasonTest extends TestBase {
  "Reason" - {
    ".validate" - {
      "returns errors when body is empty" in {
        val reason = Factories.reason.copy(body = "")
        val result = reason.validate

        result.messages must have size 1
        result.messages.head mustBe "body must not be empty"
      }
    }
  }
}
