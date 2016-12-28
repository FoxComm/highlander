package models

import models.account.User
import models.auth.UserToken
import models.payment.giftcard.GiftCard
import services.Authenticator.AuthData
import testutils.CustomMatchers._
import testutils.TestBase
import utils.aliases.AU
import utils.seeds.Seeds.Factories

class GiftCardTest extends TestBase {

  private implicit val totallyFakeAuthData: AU =
    AuthData[User](UserToken(0, None, None, Seq.empty, "foo", 1, Map.empty), null, null)

  "GiftCard" - {
    ".validate" - {
      "returns errors when canceled with no corresponding reason" in {
        val gc     = Factories.giftCard.copy(state = GiftCard.Canceled)
        val result = gc.validate
        invalidValue(result) must includeFailure("canceledAmount must be present when canceled")
      }

      "returns errors when balances >= 0" in {
        val gc     = Factories.giftCard.copy(originalBalance = 0, currentBalance = -1)
        val result = gc.validate
        invalidValue(result) must includeFailure(
            "currentBalance should be greater or equal than zero")
      }
    }
  }
}
