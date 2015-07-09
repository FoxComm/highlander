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
    "debit must be greater than zero" in new Fixture {
      val sc = (for {
        origin ← StoreCreditCsrs.save(Factories.storeCreditCsr.copy(adminId = admin.id))
        sc ← StoreCredits.save(Factories.storeCredit.copy(originId = origin.id))
      } yield sc).run().futureValue

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

    "updates the StoreCredit's currentBalance and availableBalance after insert" in new Fixture {
      val sc = (for {
        origin ← StoreCreditCsrs.save(Factories.storeCreditCsr.copy(adminId = admin.id))
        sc ← StoreCredits.save(Factories.storeCredit.copy(originalBalance = 500, originId = origin.id))
        _ ← StoreCredits.debit(storeCredit = sc, debit = 50, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 25, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 15, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 10, capture = true)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 100, capture = false)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 50, capture = false)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 50, capture = false)
        _ ← StoreCredits.debit(storeCredit = sc, debit = 200, capture = true)
        sc ← StoreCredits.findById(sc.id)
      } yield sc.get).run().futureValue

      List(sc.availableBalance, sc.currentBalance) must === (List(0, 200))
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (admin, customer) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
    } yield (admin, customer)).run().futureValue
  }
}

