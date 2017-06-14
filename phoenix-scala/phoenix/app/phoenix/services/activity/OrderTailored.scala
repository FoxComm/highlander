package phoenix.services.activity

import java.time.Instant

import core.utils.Money.Currency
import phoenix.models.Note
import phoenix.models.cord.Order
import phoenix.responses.cord.OrderResponse
import phoenix.responses.users.UserResponse

object OrderTailored {

  case class OrderStateChanged(admin: UserResponse, order: OrderResponse, oldState: Order.State)
      extends ActivityBase[OrderStateChanged]

  case class OrderRemorsePeriodIncreased(admin: UserResponse,
                                         order: OrderResponse,
                                         oldPeriodEnd: Option[Instant])
      extends ActivityBase[OrderRemorsePeriodIncreased]

  case class OrderBulkStateChanged(admin: Option[UserResponse],
                                   cordRefNums: Seq[String],
                                   newState: Order.State)
      extends ActivityBase[OrderBulkStateChanged]

  /* Order checkout & order payments */
  case class OrderCheckoutCompleted(order: OrderResponse) extends ActivityBase[OrderCheckoutCompleted]

  case class OrderCaptured(accountId: Int,
                           orderNum: String,
                           captured: Long,
                           external: Long,
                           internal: Long,
                           lineItems: Long,
                           taxes: Long,
                           shipping: Long,
                           currency: Currency)
      extends ActivityBase[OrderCaptured]

  case class CreditCardAuthCompleted(accountId: Int,
                                     cordRef: String,
                                     orderNum: String,
                                     amount: Long,
                                     currency: Currency,
                                     cardId: Int)
      extends ActivityBase[CreditCardAuthCompleted]

  case class ApplePayAuthCompleted(accountId: Int, stripeTokenId: String, amount: Long, currency: Currency)
      extends ActivityBase[ApplePayAuthCompleted]

  case class CreditCardChargeCompleted(accountId: Int,
                                       cordRef: String,
                                       orderNum: String,
                                       amount: Long,
                                       currency: Currency,
                                       cardId: Int)
      extends ActivityBase[CreditCardChargeCompleted]

  /* Order Notes */
  case class OrderNoteCreated(admin: UserResponse, order: Order, note: Note)
      extends ActivityBase[OrderNoteCreated]

  case class OrderNoteUpdated(admin: UserResponse, order: Order, oldNote: Note, note: Note)
      extends ActivityBase[OrderNoteUpdated]

  case class OrderNoteDeleted(admin: UserResponse, order: Order, note: Note)
      extends ActivityBase[OrderNoteDeleted]

}
