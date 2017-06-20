package phoenix.utils.seeds

import core.db._
import core.utils.Money.Currency
import phoenix.models.account.Scope
import phoenix.models.payment.storecredit._
import phoenix.utils.aliases._

import scala.concurrent.ExecutionContext.Implicits.global

trait StoreCreditSeeds {

  def createStoreCredits(adminId: Int, cust1: Int, cust3: Int)(implicit au: AU): DbResultT[Unit] =
    for {
      _      ← * <~ StoreCreditSubtypes.createAll(storeCreditSubTypes)
      origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = adminId, reasonId = 1))
      newSc = storeCredit.copy(originId = origin.id)
      sc1 ← * <~ StoreCredits.create(newSc.copy(accountId = cust1))
      sc2 ← * <~ StoreCredits.create(newSc.copy(originalBalance = 1000, accountId = cust1))
      sc3 ← * <~ StoreCredits.create(newSc.copy(originalBalance = 500, accountId = cust1))
      sc4 ← * <~ StoreCredits.create(newSc.copy(originalBalance = 2000, accountId = cust3))
    } yield {}

  def storeCredit(implicit au: AU) =
    StoreCredit(accountId = 0,
                scope = Scope.current,
                originId = 0,
                originType = StoreCredit.CsrAppeasement,
                originalBalance = 5000,
                currency = Currency.USD)

  def storeCreditSubTypes: Seq[StoreCreditSubtype] = Seq(
    StoreCreditSubtype(title = "Appeasement Subtype A", originType = StoreCredit.CsrAppeasement),
    StoreCreditSubtype(title = "Appeasement Subtype B", originType = StoreCredit.CsrAppeasement),
    StoreCreditSubtype(title = "Appeasement Subtype C", originType = StoreCredit.CsrAppeasement)
  )
}
