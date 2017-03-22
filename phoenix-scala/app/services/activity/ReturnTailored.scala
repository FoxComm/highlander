package services.activity

import models.Note
import models.cord.OrderPayment
import models.payment.PaymentMethod
import models.returns.{Return, ReturnLineItem, ReturnReason}
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

  case class ReturnPaymentAdded(rma: ReturnResponse, payment: OrderPayment)
      extends ActivityBase[ReturnPaymentAdded]

  case class ReturnPaymentDeleted(rma: ReturnResponse, paymentMethod: PaymentMethod.Type)
      extends ActivityBase[ReturnPaymentDeleted]

}
