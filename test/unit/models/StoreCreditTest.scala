package models

import util.TestBase
import utils.Seeds.Factories

class StoreCreditTest extends TestBase {
  "GiftCard" - {
    ".validate" - {
      "returns errors when canceled with no corresponding reason" in {
        val sc = Factories.storeCredit.copy(status = StoreCredit.Canceled)
        val result = sc.validate

        result.messages must have size 1
        result.messages.head mustBe "canceledReason must not be empty"
      }
    }
  }
}
