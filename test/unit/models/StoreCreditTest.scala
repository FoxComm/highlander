package models

import cats.data.{NonEmptyList => NEL}
import models.payment.storecredit.StoreCredit
import services.GeneralFailure
import util.TestBase
import utils.seeds.Seeds
import utils.seeds.Seeds.Factories

class StoreCreditTest extends TestBase {
  "StoreCredit" - {
    ".validateNew" - {
      "fails when originalBalance is less than zero" in {
        val sc = Factories.storeCredit.copy(originalBalance = -1, availableBalance = 0, currentBalance = 0)
        val result = sc.validate

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL(m.modelName)) mustBe NEL(
          GeneralFailure("originalBalance cannot be less than currentBalance"),
          GeneralFailure("originalBalance cannot be less than availableBalance"),
          GeneralFailure("originalBalance must be greater than zero")
        )
      }

      "fails when canceled with no corresponding reason" in {
        val sc = Factories.storeCredit.copy(state = StoreCredit.Canceled)
        val result = sc.validate

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL(m.modelName)) mustBe NEL(
          GeneralFailure("canceledAmount must be present when canceled")
        )
      }

      "succeeds when valid" in {
        val sc = Factories.storeCredit
        val result = sc.validate

        result mustBe 'valid
        result.toOption.value === sc
      }
    }
  }
}