package models

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import slick.driver.PostgresDriver
import util.IntegrationTestBase
import utils.Seeds.Factories

class StoreCreditAdjustmentTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "StoreCreditAdjustment" - {
    "debit must be greater than zero" in {
      val sc = StoreCredits.save(Factories.storeCredit).run().futureValue

      val adjustments = Table(
        ("adjustments"),
        (StoreCredits.debit(sc, -1, false)),
        (StoreCredits.debit(sc, 0, false))
      )

      forAll(adjustments) { adjustment ⇒
        val failure = adjustment.run().failed.futureValue
        failure.getMessage must include( """violates check constraint "valid_debit"""")
      }
    }

    "updates the StoreCredit's currentBalance after insert" in {
      val sc = (for {
        sc ← StoreCredits.save(Factories.storeCredit.copy(originalBalance = 100))
        _ ← StoreCredits.debit(storeCredit = sc, debit = 50, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 25, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 15, capture = true)
      } yield sc).run().futureValue

      StoreCredits.findById(sc.id).run().futureValue.get.currentBalance === (15)
    }
  }
}

