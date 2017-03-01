package models

import cats.data.{NonEmptyList ⇒ NEL}
import failures.GeneralFailure
import models.account.User
import models.auth.UserToken
import models.payment.storecredit.StoreCredit
import services.Authenticator.AuthData
import testutils.TestBase
import utils.aliases._
import utils.seeds.Factories

class StoreCreditTest extends TestBase {

  private implicit val totallyFakeAuthData: AU =
    AuthData[User](UserToken(0, None, None, Seq.empty, "foo", 1, Map.empty), null, null)

  "StoreCredit" - {
    ".validateNew" - {
      "fails when originalBalance is less than zero" in {
        val sc = Factories.storeCredit.copy(originalBalance = -1,
                                            availableBalance = 0,
                                            currentBalance = 0)
        val result = sc.validate

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL.of(m.modelName)) mustBe NEL.of(
            GeneralFailure("originalBalance cannot be less than currentBalance"),
            GeneralFailure("originalBalance cannot be less than availableBalance"),
            GeneralFailure("originalBalance must be greater than zero")
        )
      }

      "fails when canceled with no corresponding reason" in {
        val sc     = Factories.storeCredit.copy(state = StoreCredit.Canceled)
        val result = sc.validate

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL.of(m.modelName)) mustBe NEL.of(
            GeneralFailure("canceledAmount must be present when canceled")
        )
      }

      "succeeds when valid" in {
        val sc     = Factories.storeCredit
        val result = sc.validate

        result mustBe 'valid
        result.toOption.value === sc
      }
    }
  }
}
