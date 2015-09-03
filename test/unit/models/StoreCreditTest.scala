package models

import util.TestBase
import utils.Seeds.Factories
import services._
import StoreCredit._
import cats.data.{NonEmptyList ⇒ NEL}

class StoreCreditTest extends TestBase {
  "StoreCredit" - {
    ".validateNew" - {
      "fails when originalBalance is less than zero" in {
        val sc = Factories.storeCredit.copy(originalBalance = -1, availableBalance = 0, currentBalance = 0)
        val result = sc.validateNew

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL(m.modelName)) must === (NEL(
          "originalBalance cannot be less than currentBalance",
          "originalBalance cannot be less than availableBalance",
          "originalBalance must be greater than zero"
        ))
      }

      "fails when canceled with no corresponding reason" in {
        val sc = Factories.storeCredit.copy(status = StoreCredit.Canceled)
        val result = sc.validateNew

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL(m.modelName)) must === (NEL(
          "canceledReason must be present when canceled"
        ))
      }

      "succeeds when valid" in {
        val sc = Factories.storeCredit
        val result = sc.validateNew

        result mustBe 'valid
        result.toOption.get === sc
      }
    }
  }
}
