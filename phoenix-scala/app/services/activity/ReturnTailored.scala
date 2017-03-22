package services.activity

import models.Note
import models.cord.OrderPayment
import models.payment.PaymentMethod
import models.returns.{Return, ReturnLineItem}
import payloads.ReturnPayloads.ReturnLineItemPayload
import responses.{GiftCardResponse, ReturnResponse}
import responses.ReturnResponse.{Root ⇒ ReturnResponse}
import responses.UserResponse.{Root ⇒ UserResponse}

object ReturnTailored {

  case class ReturnCreated(admin: UserResponse, rma: ReturnResponse)
      extends ActivityBase[ReturnCreated]

  case class ReturnStateChanged(admin: UserResponse, rma: ReturnResponse, oldState: Return.State)
      extends ActivityBase[ReturnStateChanged]

  case class ReturnItemAdded(rma: ReturnResponse, payload: ReturnLineItemPayload)
      extends ActivityBase[ReturnItemAdded]

  case class ReturnItemDeleted(rma: ReturnResponse, li: ReturnLineItem)
      extends ActivityBase[ReturnItemDeleted]

  case class ReturnPaymentAdded(rma: ReturnResponse, payment: OrderPayment)
      extends ActivityBase[ReturnPaymentAdded]

  case class ReturnPaymentDeleted(rma: ReturnResponse, paymentMethod: PaymentMethod.Type)
      extends ActivityBase[ReturnPaymentDeleted]

}
