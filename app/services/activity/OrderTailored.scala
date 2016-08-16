package services.activity

import java.time.Instant

import models.Note
import models.cord.Order
import responses.StoreAdminResponse
import responses.cord.OrderResponse
import utils.Money.Currency

object OrderTailored {

  case class OrderStateChanged(admin: StoreAdminResponse.Root,
                               order: OrderResponse,
                               oldState: Order.State)
      extends ActivityBase[OrderStateChanged]

  case class OrderRemorsePeriodIncreased(admin: StoreAdminResponse.Root,
                                         order: OrderResponse,
                                         oldPeriodEnd: Option[Instant])
      extends ActivityBase[OrderRemorsePeriodIncreased]

  case class OrderBulkStateChanged(admin: StoreAdminResponse.Root,
                                   cordRefNums: Seq[String],
                                   newState: Order.State)
      extends ActivityBase[OrderBulkStateChanged]

  /* Order checkout & order payments */
  case class OrderCheckoutCompleted(order: OrderResponse)
      extends ActivityBase[OrderCheckoutCompleted]

  case class OrderCaptured(customerId: Int,
                           orderNum: String,
                           captured: Int,
                           external: Int,
                           internal: Int,
                           lineItems: Int,
                           taxes: Int,
                           shipping: Int,
                           currency: Currency)
      extends ActivityBase[OrderCaptured]

  case class CreditCardAuthCompleted(customerId: Int,
                                     cordRef: String,
                                     orderNum: String,
                                     amount: Int,
                                     currency: Currency,
                                     cardId: Int)
      extends ActivityBase[CreditCardAuthCompleted]

  case class CreditCardChargeCompleted(customerId: Int,
                                       cordRef: String,
                                       orderNum: String,
                                       amount: Int,
                                       currency: Currency,
                                       cardId: Int)
      extends ActivityBase[CreditCardChargeCompleted]

  /* Order Notes */
  case class OrderNoteCreated(admin: StoreAdminResponse.Root, order: Order, note: Note)
      extends ActivityBase[OrderNoteCreated]

  case class OrderNoteUpdated(admin: StoreAdminResponse.Root,
                              order: Order,
                              oldNote: Note,
                              note: Note)
      extends ActivityBase[OrderNoteUpdated]

  case class OrderNoteDeleted(admin: StoreAdminResponse.Root, order: Order, note: Note)
      extends ActivityBase[OrderNoteDeleted]

}
