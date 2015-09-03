package models

import util.TestBase
import utils.Seeds.Factories

class GiftCardTest extends TestBase {
  "GiftCard" - {
    ".validate" - {
      "returns errors when canceled with no corresponding reason" in {
        val gc = Factories.giftCard.copy(status = GiftCard.Canceled)
        val result = gc.validateNew
        invalidValue(result).head.description.head must include("canceledReason must not be empty")
      }

      "returns errors when balances >= 0" in {
        val gc = Factories.giftCard.copy(originalBalance = 0, currentBalance = -1)
        val result = gc.validateNew
        invalidValue(result).head.description.head must include("currentBalance should be greater or equal than zero")
      }
    }
  }
}
