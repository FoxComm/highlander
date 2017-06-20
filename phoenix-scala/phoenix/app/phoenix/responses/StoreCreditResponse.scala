package phoenix.responses

import java.time.Instant

import phoenix.models.payment.storecredit.StoreCredit
import core.utils.Money.Currency

object StoreCreditResponse {
  case class Root(id: Int,
                  originId: Int,
                  originType: StoreCredit.OriginType,
                  subTypeId: Option[Int],
                  currency: Currency,
                  customerId: Int,
                  originalBalance: Long,
                  currentBalance: Long,
                  availableBalance: Long,
                  canceledAmount: Option[Long],
                  canceledReason: Option[Int],
                  state: StoreCredit.State,
                  createdAt: Instant)
      extends ResponseItem

  case class Totals(availableBalance: Long = 0, currentBalance: Long = 0)

  case class WithTotals(storeCredits: Seq[Root], totals: Option[Totals])

  def build(records: Seq[StoreCredit]): Seq[Root] = records.map(build)

  def build(storeCredit: StoreCredit): Root =
    Root(
      id = storeCredit.id,
      originId = storeCredit.originId,
      originType = storeCredit.originType,
      subTypeId = storeCredit.subTypeId,
      currency = storeCredit.currency,
      customerId = storeCredit.accountId,
      originalBalance = storeCredit.originalBalance,
      currentBalance = storeCredit.currentBalance,
      availableBalance = storeCredit.availableBalance,
      canceledAmount = storeCredit.canceledAmount,
      canceledReason = storeCredit.canceledReason,
      state = storeCredit.state,
      createdAt = storeCredit.createdAt
    )
}
