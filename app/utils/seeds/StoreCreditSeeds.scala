package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Customer, StoreAdmin, StoreCredit, StoreCreditManual, StoreCreditManuals, StoreCreditSubtype, StoreCreditSubtypes, StoreCredits}
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency

trait StoreCreditSeeds {

  def createStoreCredits(adminId: StoreAdmin#Id, cust1: Customer#Id, cust3: Customer#Id): DbResultT[Unit] = for {
    _ ← * <~ StoreCreditSubtypes.createAll(storeCreditSubTypes)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = adminId, reasonId = 1))
    newSc = storeCredit.copy(originId = origin.id)
    sc1 ← * <~ StoreCredits.create(newSc.copy(customerId = cust1))
    sc2 ← * <~ StoreCredits.create(newSc.copy(originalBalance = 1000, customerId = cust1))
    sc3 ← * <~ StoreCredits.create(newSc.copy(originalBalance = 500, customerId = cust1))
    sc4 ← * <~ StoreCredits.create(newSc.copy(originalBalance = 2000, customerId = cust3))
  } yield {}

  def storeCredit = StoreCredit(customerId = 0, originId = 0, originType = StoreCredit.CsrAppeasement,
    originalBalance = 5000, currency = Currency.USD)

  def storeCreditSubTypes: Seq[StoreCreditSubtype] = Seq(
    StoreCreditSubtype(title = "Appeasement Subtype A", originType = StoreCredit.CsrAppeasement),
    StoreCreditSubtype(title = "Appeasement Subtype B", originType = StoreCredit.CsrAppeasement),
    StoreCreditSubtype(title = "Appeasement Subtype C", originType = StoreCredit.CsrAppeasement)
  )

}
