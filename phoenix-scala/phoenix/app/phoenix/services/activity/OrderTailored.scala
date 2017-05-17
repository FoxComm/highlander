package services.activity

import java.time.Instant

import models.Note
import models.cord.Order
import responses.UserResponse
import responses.cord.OrderResponse
import utils.Money.Currency

object OrderTailored {

  case class OrderStateChanged(admin: UserResponse.Root,
                               order: OrderResponse,
                               oldState: Order.State)
      extends ActivityBase[OrderStateChanged]

  case class OrderRemorsePeriodIncreased(admin: UserResponse.Root,
                                         order: OrderResponse,
                                         oldPeriodEnd: Option[Instant])
      extends ActivityBase[OrderRemorsePeriodIncreased]

  case class OrderBulkStateChanged(admin: Option[UserResponse.Root],
                                   cordRefNums: Seq[String],
                                   newState: Order.State)
      extends ActivityBase[OrderBulkStateChanged]

  /* Order checkout & order payments */
  case class OrderCheckoutCompleted(order: OrderResponse)
      extends ActivityBase[OrderCheckoutCompleted]

  case class OrderCaptured(accountId: Int,
                           orderNum: String,
                           captured: Int,
                           external: Int,
                           internal: Int,
                           lineItems: Int,
                           taxes: Int,
                           shipping: Int,
                           currency: Currency)
      extends ActivityBase[OrderCaptured]

  case class CreditCardAuthCompleted(accountId: Int,
                                     cordRef: String,
                                     orderNum: String,
                                     amount: Int,
                                     currency: Currency,
                                     cardId: Int)
      extends ActivityBase[CreditCardAuthCompleted]

  case class CreditCardChargeCompleted(accountId: Int,
                                       cordRef: String,
                                       orderNum: String,
                                       amount: Int,
                                       currency: Currency,
                                       cardId: Int)
      extends ActivityBase[CreditCardChargeCompleted]

  /* Order Notes */
  case class OrderNoteCreated(admin: UserResponse.Root, order: Order, note: Note)
      extends ActivityBase[OrderNoteCreated]

  case class OrderNoteUpdated(admin: UserResponse.Root, order: Order, oldNote: Note, note: Note)
      extends ActivityBase[OrderNoteUpdated]

  case class OrderNoteDeleted(admin: UserResponse.Root, order: Order, note: Note)
      extends ActivityBase[OrderNoteDeleted]

}
