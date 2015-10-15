package responses

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.GiftCard
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.Slick.implicits._

object GiftCardResponse {
  final val mockMessage = "Not implemented yet"

  final case class BulkCreateResponse(responses: Seq[Root]) extends ResponseItem

  final case class Root(
    id: Int,
    createdAt: Instant,
    code: String,
    originId: Int,
    originType: GiftCard.OriginType,
    subTypeId: Option[Int],
    status: GiftCard.Status,
    currency: Currency,
    originalBalance: Int,
    availableBalance: Int,
    currentBalance: Int,
    canceledAmount: Option[Int],
    canceledReason: Option[Int],
    customer: Option[CustomerResponse.Root],
    storeAdmin: Option[StoreAdminResponse.Root],
    message: String) extends ResponseItem

  def build(gc: GiftCard, customer: Option[CustomerResponse.Root] = None, admin: Option[StoreAdminResponse.Root] = None):
  Root =
    Root(
      id = gc.id,
      createdAt = gc.createdAt,
      code = gc.code,
      originId = gc.originId,
      originType = gc.originType,
      subTypeId = gc.subTypeId,
      status = gc.status,
      currency = gc.currency,
      originalBalance = gc.originalBalance,
      availableBalance = gc.availableBalance,
      currentBalance = gc.currentBalance,
      canceledAmount = gc.canceledAmount,
      canceledReason = gc.canceledReason,
      customer = customer,
      storeAdmin = admin,
      message = mockMessage)
}
