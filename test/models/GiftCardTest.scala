package models

import util.IntegrationTestBase
import utils.Seeds.Factories

class GiftCardTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "GiftCard" - {
    ".validate" - {
      "returns errors when canceled with no corresponding reason" in {
        val gc = Factories.giftCard.copy(status = GiftCard.Canceled)
        val result = gc.validate

        result.messages must have size 1
        result.messages.head mustBe "canceledReason must not be empty"
      }

      "returns errors when balances >= 0" in {
        val gc = Factories.giftCard.copy(originalBalance = 0, currentBalance = -1)
        val result = gc.validate

        result.messages must have size 1
        result.messages.head must include ("currentBalance got -1")
      }
    }
  }
}
