package models

import util.TestBase
import utils.Seeds.Factories
import util.CustomMatchers._

class GiftCardTest extends TestBase {
  "GiftCard" - {
    ".validate" - {
      "returns errors when canceled with no corresponding reason" in {
        val gc = Factories.giftCard.copy(status = GiftCard.Canceled)
        val result = gc.validate
        invalidValue(result) must includeFailure("canceledAmount must be present when canceled")
      }

      "returns errors when balances >= 0" in {
        val gc = Factories.giftCard.copy(originalBalance = 0, currentBalance = -1)
        val result = gc.validate
        invalidValue(result) must includeFailure("currentBalance should be greater or equal than zero")
      }
    }
  }
}
