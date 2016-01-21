package services.activity

import models.{ShippingMethod, OrderShippingAddress, PaymentMethod, Order, Note}
import responses.{CreditCardsResponse, FullOrder, GiftCardResponse, StoreAdminResponse}

object OrderTailored {
  final case class CartCreated(admin: StoreAdminResponse.Root, order: FullOrder.Root)
    extends ActivityBase[CartCreated]

  final case class OrderStateChanged(admin: StoreAdminResponse.Root, order: FullOrder.Root, oldState: Order.Status)
    extends ActivityBase[OrderStateChanged]

  final case class OrderBulkStateChanged(admin: StoreAdminResponse.Root, ordersRefNums: Seq[String],
    newState: Order.Status)
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
  final case class OrderShippingMethodUpdated(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    shippingMethod: Option[ShippingMethod])
    extends ActivityBase[OrderShippingMethodUpdated]

  final case class OrderShippingMethodRemoved(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    shippingMethod: ShippingMethod)
    extends ActivityBase[OrderShippingMethodRemoved]

  /* Order Shipping Addresses */
  final case class OrderShippingAddressAdded(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    shippingAddress: OrderShippingAddress)
    extends ActivityBase[OrderShippingAddressAdded]

  final case class OrderShippingAddressUpdated(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    shippingAddress: OrderShippingAddress)
    extends ActivityBase[OrderShippingAddressUpdated]

  final case class OrderShippingAddressRemoved(admin: StoreAdminResponse.Root, order: FullOrder.Root)
    extends ActivityBase[OrderShippingAddressRemoved]

  /* Order Payment Methods */
  final case class OrderPaymentMethodAddedCreditCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    creditCard: CreditCardsResponse.Root)
    extends ActivityBase[OrderPaymentMethodAddedCreditCard]

  final case class OrderPaymentMethodAddedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root, amount: Int)
    extends ActivityBase[OrderPaymentMethodAddedGiftCard]

  final case class OrderPaymentMethodAddedStoreCredit(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    amount: Int)
    extends ActivityBase[OrderPaymentMethodAddedStoreCredit]

  final case class OrderPaymentMethodDeleted(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    pmt: PaymentMethod.Type)
    extends ActivityBase[OrderPaymentMethodDeleted]

  final case class OrderPaymentMethodDeletedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    giftCard: GiftCardResponse.Root)
    extends ActivityBase[OrderPaymentMethodDeletedGiftCard]

  /* Order Notes */
  final case class OrderNoteCreated(admin: StoreAdminResponse.Root, order: Order, note: Note)
    extends ActivityBase[OrderNoteCreated]

  final case class OrderNoteUpdated(admin: StoreAdminResponse.Root, order: Order, oldNote: Note, note: Note)
    extends ActivityBase[OrderNoteUpdated]

  final case class OrderNoteDeleted(admin: StoreAdminResponse.Root, order: Order, note: Note)
    extends ActivityBase[OrderNoteDeleted]
}