package services.activity

import java.time.Instant

import models.order.Order
import models.payment.PaymentMethod
import models.shipping.ShippingMethod
import models.Note
import responses.order.FullOrder
import responses.{Addresses, CreditCardsResponse, GiftCardResponse, StoreAdminResponse}
import utils.Money.Currency

object OrderTailored {
  case class CartCreated(admin: Option[StoreAdminResponse.Root], order: FullOrder.Root)
    extends ActivityBase[CartCreated]

  case class OrderStateChanged(admin: StoreAdminResponse.Root, order: FullOrder.Root, oldState: Order.State)
    extends ActivityBase[OrderStateChanged]

  case class OrderRemorsePeriodIncreased(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    oldPeriodEnd: Option[Instant]) extends ActivityBase[OrderRemorsePeriodIncreased]

  case class OrderBulkStateChanged(admin: StoreAdminResponse.Root, orderRefNums: Seq[String],
    newState: Order.State)
    extends ActivityBase[OrderBulkStateChanged]

  /* Order Line Items */
  case class OrderLineItemsAddedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root)
    extends ActivityBase[OrderLineItemsAddedGiftCard]

  case class OrderLineItemsUpdatedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root)
    extends ActivityBase[OrderLineItemsUpdatedGiftCard]

  case class OrderLineItemsDeletedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root)
    extends ActivityBase[OrderLineItemsDeletedGiftCard]

  case class OrderLineItemsUpdatedQuantities(order: FullOrder.Root, oldQuantities: Map[String, Int],
    newQuantities: Map[String, Int], admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderLineItemsUpdatedQuantities]

  /* Order checkout & order payments */
  case class OrderCheckoutCompleted(order: FullOrder.Root)
    extends ActivityBase[OrderCheckoutCompleted]

  case class CreditCardChargeCompleted(customerId: Int, orderId: Int, orderNum: String,
    amount: Int, currency: Currency, cardId: Int)
    extends ActivityBase[CreditCardChargeCompleted]

  /* Order Shipping Methods */
  case class OrderShippingMethodUpdated(order: FullOrder.Root, shippingMethod: Option[ShippingMethod],
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingMethodUpdated]

  case class OrderShippingMethodRemoved(order: FullOrder.Root, shippingMethod: ShippingMethod,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingMethodRemoved]

  /* Order Shipping Addresses */
  case class OrderShippingAddressAdded(order: FullOrder.Root, shippingAddress: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingAddressAdded]

  case class OrderShippingAddressUpdated(order: FullOrder.Root, shippingAddress: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingAddressUpdated]

  case class OrderShippingAddressRemoved(order: FullOrder.Root, shippingAddress: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingAddressRemoved]

  /* Order Payment Methods */
  case class OrderPaymentMethodAddedCreditCard(order: FullOrder.Root, creditCard: CreditCardsResponse.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodAddedCreditCard]

  case class OrderPaymentMethodAddedGiftCard(order: FullOrder.Root, giftCard: GiftCardResponse.Root, amount: Int,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodAddedGiftCard]

  case class OrderPaymentMethodUpdatedGiftCard(order: FullOrder.Root, giftCard: GiftCardResponse.Root,
    oldAmount: Option[Int], amount: Int, admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodUpdatedGiftCard]

  case class OrderPaymentMethodAddedStoreCredit(order: FullOrder.Root, amount: Int,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodAddedStoreCredit]

  case class OrderPaymentMethodDeleted(order: FullOrder.Root, pmt: PaymentMethod.Type,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodDeleted]

  case class OrderPaymentMethodDeletedGiftCard(order: FullOrder.Root, giftCard: GiftCardResponse.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodDeletedGiftCard]

  /* Order Notes */
  case class OrderNoteCreated(admin: StoreAdminResponse.Root, order: Order, note: Note)
    extends ActivityBase[OrderNoteCreated]

  case class OrderNoteUpdated(admin: StoreAdminResponse.Root, order: Order, oldNote: Note, note: Note)
    extends ActivityBase[OrderNoteUpdated]

  case class OrderNoteDeleted(admin: StoreAdminResponse.Root, order: Order, note: Note)
    extends ActivityBase[OrderNoteDeleted]
}
