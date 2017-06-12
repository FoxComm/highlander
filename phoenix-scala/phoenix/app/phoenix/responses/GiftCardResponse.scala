package phoenix.responses

import java.time.Instant

import phoenix.models.payment.giftcard._
import core.utils.Money._

object GiftCardResponse {

  final val mockMessage = "Not implemented yet"

  case class Root(id: Int,
                  createdAt: Instant,
                  code: String,
                  originId: Int,
                  originType: GiftCard.OriginType,
                  subTypeId: Option[Int],
                  state: GiftCard.State,
                  currency: Currency,
                  originalBalance: Long,
                  availableBalance: Long,
                  currentBalance: Long,
                  canceledAmount: Option[Long],
                  canceledReason: Option[Int],
                  customer: Option[CustomerResponse.Root],
                  storeAdmin: Option[UserResponse.Root],
                  senderName: Option[String] = None,
                  recipientName: Option[String] = None,
                  recipientEmail: Option[String] = None,
                  message: Option[String] = None)
      extends ResponseItem

  def build(gc: GiftCard,
            customer: Option[CustomerResponse.Root] = None,
            admin: Option[UserResponse.Root] = None): Root =
    Root(
      id = gc.id,
      createdAt = gc.createdAt,
      code = gc.code,
      originId = gc.originId,
      originType = gc.originType,
      subTypeId = gc.subTypeId,
      state = gc.state,
      currency = gc.currency,
      originalBalance = gc.originalBalance,
      availableBalance = gc.availableBalance,
      currentBalance = gc.currentBalance,
      canceledAmount = gc.canceledAmount,
      canceledReason = gc.canceledReason,
      customer = customer,
      storeAdmin = admin,
      senderName = gc.senderName,
      recipientName = gc.recipientName,
      recipientEmail = gc.recipientEmail,
      message = gc.message
    )
}
