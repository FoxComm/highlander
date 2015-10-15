package responses

import java.time.Instant
import slick.driver.PostgresDriver.api._
import utils.Money.Currency

object StoreCreditResponse {
  final case class Root(
    id: Int,
    originId: Int,
    originType: models.StoreCredit.OriginType,
    subTypeId: Option[Int],
    currency: Currency,
    customerId: Int,
    originalBalance: Int,
    currentBalance: Int,
    availableBalance: Int,
    canceledAmount: Option[Int],
    canceledReason: Option[Int],
    status: models.StoreCredit.Status,
    createdAt: Instant) extends ResponseItem

  def build(records: Seq[models.StoreCredit]): Seq[Root] = records.map(build)

  def build(storeCredit: models.StoreCredit): Root = {
    Root(id = storeCredit.id,
      originId = storeCredit.originId,
      originType = storeCredit.originType,
      subTypeId = storeCredit.subTypeId,
      currency = storeCredit.currency,
      customerId = storeCredit.customerId,
      originalBalance = storeCredit.originalBalance,
      currentBalance = storeCredit.currentBalance,
      availableBalance = storeCredit.availableBalance,
      canceledAmount = storeCredit.canceledAmount,
      canceledReason = storeCredit.canceledReason,
      status = storeCredit.status,
      createdAt = storeCredit.createdAt)
  }
}
