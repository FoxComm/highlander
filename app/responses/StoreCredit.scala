package responses

import scala.concurrent.{ExecutionContext, Future}

import models.{StoreCreditAdjustment, StoreCreditManual}
import org.joda.time.DateTime
import utils.Money.Currency

object StoreCredit {
  final case class Root(
    id: Int,
    customerId: Int,
    currency: Currency,
    status: models.StoreCredit.Status,
    originalBalance: Int,
    currentBalance: Int,
    availableBalance: Int,
    canceledReason: Option[String],
    createdBy: Int,
    createdAt: DateTime,
    transactions: List[StoreCreditAdjustment] = List.empty)

  def build(sc: models.StoreCredit, manual: StoreCreditManual,
    adjustments: List[StoreCreditAdjustment] = List.empty): Root =
    Root(id = sc.id, customerId = sc.customerId, currency = sc.currency, status = sc.status,
      originalBalance = sc.originalBalance, currentBalance = sc.currentBalance, availableBalance = sc.availableBalance,
      canceledReason = sc.canceledReason, createdBy = manual.adminId, createdAt = sc.createdAt,
      transactions = adjustments)
}
