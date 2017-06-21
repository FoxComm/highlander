package phoenix.responses.giftcards

import java.time.Instant

import core.utils.Money._
import phoenix.models.payment.giftcard._
import phoenix.responses.ResponseItem
import phoenix.responses.users.{CustomerResponse, UserResponse}

case class GiftCardResponse(id: Int,
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
                            customer: Option[CustomerResponse],
                            storeAdmin: Option[UserResponse],
                            senderName: Option[String] = None,
                            recipientName: Option[String] = None,
                            recipientEmail: Option[String] = None,
                            message: Option[String] = None)
    extends ResponseItem

object GiftCardResponse {

  final val mockMessage = "Not implemented yet"

  def build(gc: GiftCard,
            customer: Option[CustomerResponse] = None,
            admin: Option[UserResponse] = None): GiftCardResponse =
    GiftCardResponse(
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
