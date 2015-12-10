package services.activity

import models.{OrderShippingMethod, OrderShippingAddress, PaymentMethod, GiftCard, StoreCredit, Order}
import payloads.UpdateLineItemsPayload
import responses.{AdminNotes, Addresses, CustomerResponse, CreditCardsResponse, FullOrder, GiftCardResponse,
StoreCreditResponse, ShippingMethods, StoreAdminResponse}

/* Assignments */

final case class AssignedToOrder(storeAdminId: Int, orderRefNum: String)
  extends ActivityBase[AssignedToOrder]

final case class UnassignedFromOrder(storeAdminId: Int, orderRefNum: String)
  extends ActivityBase[UnassignedFromOrder]

final case class AssignedToRma(storeAdminId: Int, rmaRefNum: String)
  extends ActivityBase[AssignedToRma]

final case class UnassignedFromRma(storeAdminId: Int, rmaRefNum: String)
  extends ActivityBase[UnassignedFromRma]

/* Customers */

final case class CustomerCreated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
  extends ActivityBase[CustomerCreated]

final case class CustomerRegistered(customer: CustomerResponse.Root)
  extends ActivityBase[CustomerRegistered]

final case class CustomerActivated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
  extends ActivityBase[CustomerActivated]

final case class CustomerBlacklisted(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
  extends ActivityBase[CustomerBlacklisted]

final case class CustomerRemovedFromBlacklist(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
  extends ActivityBase[CustomerRemovedFromBlacklist]

final case class CustomerEnabled(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
  extends ActivityBase[CustomerEnabled]

final case class CustomerDisabled(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
  extends ActivityBase[CustomerDisabled]

final case class CustomerUpdated(admin: StoreAdminResponse.Root, oldInfo: CustomerResponse.Root,
  newInfo: CustomerResponse.Root)
  extends ActivityBase[CustomerUpdated]

/* Customer Addresses */

final case class CustomerAddressCreatedByAdmin(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
  address: Addresses.Root)
  extends ActivityBase[CustomerAddressCreatedByAdmin]

final case class CustomerAddressCreated(customer: CustomerResponse.Root, address: Addresses.Root)
  extends ActivityBase[CustomerAddressCreated]

final case class CustomerAddressUpdated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
  oldInfo: Addresses.Root, newInfo: Addresses.Root)
  extends ActivityBase[CustomerAddressUpdated]

final case class CustomerAddressDeleted(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
  address: Addresses.Root)
  extends ActivityBase[CustomerAddressDeleted]

/* Customer Credit Cards */

final case class CreditCardAdded(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
  creditCard: CreditCardsResponse.Root)
  extends ActivityBase[CreditCardAdded]

final case class CreditCardUpdated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
  oldInfo: CreditCardsResponse.Root, newInfo: CreditCardsResponse.Root) extends ActivityBase[CreditCardUpdated]

final case class CreditCardRemoved(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
  creditCard: CreditCardsResponse.Root)
  extends ActivityBase[CreditCardRemoved]

/* Orders */

final case class OrderCreated(admin: StoreAdminResponse.Root, order: FullOrder.Root)
  extends ActivityBase[OrderCreated]

final case class OrderStateChanged(admin: StoreAdminResponse.Root, order: FullOrder.Root)
  extends ActivityBase[OrderStateChanged]

/* Order Line Items */

final case class OrderLineItemsAddedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
  gc: GiftCardResponse.Root)
  extends ActivityBase[OrderLineItemsAddedGiftCard]

final case class OrderLineItemsUpdatedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
  gc: GiftCardResponse.Root)
  extends ActivityBase[OrderLineItemsUpdatedGiftCard]

final case class OrderLineItemsDeletedGiftCard(admin: StoreAdminResponse.Root, order: FullOrder.Root,
  gc: GiftCardResponse.Root)
  extends ActivityBase[OrderLineItemsDeletedGiftCard]

final case class OrderLineItemsUpdatedQuantities(admin: StoreAdminResponse.Root, order: FullOrder.Root,
  quantities: Seq[UpdateLineItemsPayload])
  extends ActivityBase[OrderLineItemsUpdatedQuantities]

final case class OrderLineItemsUpdatedQuantitiesByCustomer(customer: CustomerResponse.Root, order: FullOrder.Root,
  quantities: Seq[UpdateLineItemsPayload])
  extends ActivityBase[OrderLineItemsUpdatedQuantitiesByCustomer]

/* Order Shipping Methods */

final case class OrderShippingMethodUpdated(admin: StoreAdminResponse.Root, order: FullOrder.Root,
  method: OrderShippingMethod)
  extends ActivityBase[OrderShippingMethodUpdated]

final case class OrderShippingMethodRemoved(admin: StoreAdminResponse.Root, order: FullOrder.Root)
  extends ActivityBase[OrderShippingMethodRemoved]

/* Order Shipping Addresses */

final case class OrderShippingAddressAdded(admin: StoreAdminResponse.Root, order: FullOrder.Root,
  address: OrderShippingAddress)
  extends ActivityBase[OrderShippingAddressAdded]

final case class OrderShippingAddressUpdated(admin: StoreAdminResponse.Root, order: FullOrder.Root,
  address: OrderShippingAddress)
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

final case class OrderNoteAdded(orderRefNum: String, newInfo: AdminNotes.Root)
  extends ActivityBase[OrderNoteAdded]

final case class OrderNoteUpdated(orderRefNum: String, oldInfo: AdminNotes.Root, newInfo: AdminNotes.Root)
  extends ActivityBase[OrderNoteUpdated]

final case class OrderNoteRemoved(orderRefNum: String, oldInfo: AdminNotes.Root)
  extends ActivityBase[OrderNoteRemoved]

/* Gift Cards */

final case class GiftCardCreated(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root)
  extends ActivityBase[GiftCardCreated]

final case class GiftCardStateChanged(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root,
  payload: payloads.GiftCardUpdateStatusByCsr)
  extends ActivityBase[GiftCardStateChanged]

final case class GiftCardConvertedToStoreCredit(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root,
  storeCredit: StoreCreditResponse.Root)
  extends ActivityBase[GiftCardConvertedToStoreCredit]

final case class GiftCardAuthorizedFunds(giftCard: GiftCardResponse.Root, orderRefNum: String, amount: Int)
  extends ActivityBase[GiftCardAuthorizedFunds]

final case class GiftCardCapturedFunds(giftCard: GiftCardResponse.Root, orderRefNum: String, amount: Int)
  extends ActivityBase[GiftCardCapturedFunds]

/* Store Credits */

final case class StoreCreditCreated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
  storeCredit: StoreCreditResponse.Root)
  extends ActivityBase[StoreCreditCreated]

final case class StoreCreditStateChanged(admin: StoreAdminResponse.Root, storeCredit: StoreCreditResponse.Root,
  payload: payloads.StoreCreditUpdateStatusByCsr)
  extends ActivityBase[StoreCreditStateChanged]

final case class StoreCreditConvertedToGiftCard(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root,
  storeCredit: StoreCreditResponse.Root)
  extends ActivityBase[StoreCreditConvertedToGiftCard]

final case class StoreCreditAuthorizedFunds(storeCredit: StoreCreditResponse.Root, orderRefNum: String, amount: Int)
  extends ActivityBase[StoreCreditAuthorizedFunds]

final case class StoreCreditCapturedFunds(storeCredit: StoreCreditResponse.Root, orderRefNum: String, amount: Int)
  extends ActivityBase[StoreCreditCapturedFunds]