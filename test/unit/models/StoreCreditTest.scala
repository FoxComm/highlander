package models

import util.TestBase
import utils.Seeds.Factories
import StoreCredit._
import cats.data.{NonEmptyList ⇒ NEL}

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

    ".validateNew" - {
      "returns all errors when everything is wrong!!!!" in {
        val sc = Factories.storeCredit.copy(originalBalance = -1, availableBalance = 100, currentBalance = 100,
          status = Canceled, canceledReason = None)
        val result = sc.validateNew

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL(m.modelName)) must ===(NEL(
          "canceledReason must be present when canceled",
          "originalBalance cannot be less than currentBalance",
          "originalBalance cannot be less than availableBalance",
          "originalBalance must be greater than zero"
        ))
      }

      "returns an errs when something is wrong!!!!" in {
        val sc = Factories.storeCredit.copy(originalBalance = 50, availableBalance = 100, currentBalance = 50)
        val result = sc.validateNew

        result mustBe 'invalid
        result.fold(identity, m ⇒ NEL(m.modelName)) must ===(NEL(
          "originalBalance cannot be less than availableBalance"
        ))
      }

      "is right when everything is okay!!!" in {
        val sc = Factories.storeCredit
        val result = sc.validateNew
        result mustBe 'valid
        result.toOption.get === (sc)
      }
    }
  }
}
