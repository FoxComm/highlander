package responses

import java.time.Instant
import slick.driver.PostgresDriver.api._
import utils.Money.Currency

object StoreCreditResponse {
  final case class Root(
    id: Int,
    originId: Int,
    `type`: models.StoreCredit.OriginType,
    currency: Currency,
    customerId: Int,
    originalBalance: Int,
    currentBalance: Int,
    availableBalance: Int,
    status: models.StoreCredit.Status,
    createdAt: Instant,
    canceledReason: Option[String] = None)

  def build(storeCredit: models.StoreCredit): Root = {
    Root(id = storeCredit.id,
      originId = storeCredit.originId,
      `type` = storeCredit.originType,
      currency = storeCredit.currency,
      customerId = storeCredit.customerId,
      originalBalance = storeCredit.originalBalance,
      currentBalance = storeCredit.currentBalance,
      availableBalance = storeCredit.availableBalance,
      status = storeCredit.status,
      createdAt = storeCredit.createdAt,
      canceledReason = storeCredit.canceledReason
    )
  }
}
