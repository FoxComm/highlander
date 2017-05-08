package services.activity

import models.Note
import models.account.User
import models.cord.OrderPayment
import models.payment.PaymentMethod
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.returns._
import payloads.ReturnPayloads.{ReturnLineItemPayload, ReturnShippingCostLineItemPayload, ReturnSkuLineItemPayload}
import responses.{GiftCardResponse, ReturnResponse}
import responses.ReturnResponse.{Root ⇒ ReturnResponse}
import responses.UserResponse.{Root ⇒ UserResponse}

object ReturnTailored {

  case class ReturnCreated(admin: UserResponse, rma: ReturnResponse)
      extends ActivityBase[ReturnCreated]

  case class ReturnStateChanged(admin: UserResponse, rma: ReturnResponse, oldState: Return.State)
      extends ActivityBase[ReturnStateChanged]

  case class ReturnShippingCostItemAdded(rma: Return,
                                         reason: ReturnReason,
                                         payload: ReturnShippingCostLineItemPayload)
      extends ActivityBase[ReturnShippingCostItemAdded]

  case class ReturnSkuLineItemAdded(rma: Return,
                                    reason: ReturnReason,
                                    payload: ReturnSkuLineItemPayload)
      extends ActivityBase[ReturnSkuLineItemAdded]

  case class ReturnShippingCostItemDeleted(li: ReturnLineItem)
      extends ActivityBase[ReturnShippingCostItemDeleted]

  case class ReturnSkuLineItemDeleted(li: ReturnLineItem)
      extends ActivityBase[ReturnSkuLineItemDeleted]

  case class ReturnSkuLineItemsDropped(skus: List[ReturnLineItemSku])
      extends ActivityBase[ReturnSkuLineItemsDropped]

  case class ReturnPaymentsAdded(rma: ReturnResponse, payments: List[PaymentMethod.Type])
      extends ActivityBase[ReturnPaymentsAdded]

  case class ReturnPaymentsDeleted(rma: ReturnResponse, payments: List[PaymentMethod.Type])
      extends ActivityBase[ReturnPaymentsDeleted]

  case class ReturnIssueCcRefund(rma: Return, payment: ReturnPayment)
      extends ActivityBase[ReturnIssueCcRefund]

  case class ReturnIssueGcRefund(customer: User, rma: Return, gc: GiftCard)
      extends ActivityBase[ReturnIssueGcRefund]

  case class ReturnIssueScRefund(customer: User, rma: Return, sc: StoreCredit)
      extends ActivityBase[ReturnIssueScRefund]

  case class ReturnCancelRefund(rma: Return) extends ActivityBase[ReturnCancelRefund]

}
