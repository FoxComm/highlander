package models

import phoenix.models.account.User
import phoenix.models.auth.UserToken
import phoenix.models.payment.giftcard.GiftCard
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases.AU
import phoenix.utils.seeds.Factories
import testutils.CustomMatchers._
import testutils.TestBase

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
        invalidValue(result) must includeFailure("currentBalance should be greater or equal than zero")
      }
    }
  }
}
