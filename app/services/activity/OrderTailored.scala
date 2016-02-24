package services.activity

import java.time.Instant

import models.order.Order
import models.payment.PaymentMethod
import models.shipping.ShippingMethod
import models.Note
import responses.order.FullOrder
import responses.{Addresses, CreditCardsResponse, GiftCardResponse, StoreAdminResponse}

object OrderTailored {
  final case class CartCreated(admin: Option[StoreAdminResponse.Root], order: FullOrder.Root)
    extends ActivityBase[CartCreated]

  final case class OrderStateChanged(admin: StoreAdminResponse.Root, order: FullOrder.Root, oldState: Order.State)
    extends ActivityBase[OrderStateChanged]

  final case class OrderRemorsePeriodIncreased(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    oldPeriodEnd: Option[Instant]) extends ActivityBase[OrderRemorsePeriodIncreased]

  final case class OrderBulkStateChanged(admin: StoreAdminResponse.Root, orderRefNums: Seq[String],
    newState: Order.State)
    extends ActivityBase[OrderBulkStateChanged]

  /* Order Line Items */
  final case class OrderLineItemsAddedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root)
    extends ActivityBase[OrderLineItemsAddedGiftCard]

  final case class OrderLineItemsUpdatedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root)
    extends ActivityBase[OrderLineItemsUpdatedGiftCard]

  final case class OrderLineItemsDeletedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root)
    extends ActivityBase[OrderLineItemsDeletedGiftCard]

  final case class OrderLineItemsUpdatedQuantities(order: FullOrder.Root, oldQuantities: Map[String, Int],
    newQuantities: Map[String, Int], admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderLineItemsUpdatedQuantities]

  /* Order Shipping Methods */
  final case class OrderShippingMethodUpdated(order: FullOrder.Root, shippingMethod: Option[ShippingMethod],
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingMethodUpdated]

  final case class OrderShippingMethodRemoved(order: FullOrder.Root, shippingMethod: ShippingMethod,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingMethodRemoved]

  /* Order Shipping Addresses */
  final case class OrderShippingAddressAdded(order: FullOrder.Root, shippingAddress: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingAddressAdded]

  final case class OrderShippingAddressUpdated(order: FullOrder.Root, shippingAddress: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingAddressUpdated]

  final case class OrderShippingAddressRemoved(order: FullOrder.Root, shippingAddress: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderShippingAddressRemoved]

  /* Order Payment Methods */
  final case class OrderPaymentMethodAddedCreditCard(order: FullOrder.Root, creditCard: CreditCardsResponse.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodAddedCreditCard]

  final case class OrderPaymentMethodAddedGiftCard(order: FullOrder.Root, giftCard: GiftCardResponse.Root, amount: Int,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodAddedGiftCard]

  final case class OrderPaymentMethodAddedStoreCredit(order: FullOrder.Root, amount: Int,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodAddedStoreCredit]

  final case class OrderPaymentMethodDeleted(order: FullOrder.Root, pmt: PaymentMethod.Type,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodDeleted]

  final case class OrderPaymentMethodDeletedGiftCard(order: FullOrder.Root, giftCard: GiftCardResponse.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[OrderPaymentMethodDeletedGiftCard]

  /* Order Notes */
  final case class OrderNoteCreated(admin: StoreAdminResponse.Root, order: Order, note: Note)
    extends ActivityBase[OrderNoteCreated]

  final case class OrderNoteUpdated(admin: StoreAdminResponse.Root, order: Order, oldNote: Note, note: Note)
    extends ActivityBase[OrderNoteUpdated]

  final case class OrderNoteDeleted(admin: StoreAdminResponse.Root, order: Order, note: Note)
    extends ActivityBase[OrderNoteDeleted]
}
