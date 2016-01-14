package services

import scala.concurrent.ExecutionContext

import models.{PaymentMethod, StoreCredit, GiftCard, CreditCard, ShippingMethod, OrderShippingAddress,
Region, Address, Customer, StoreAdmin, Order}
import models.activity.{Activity, Activities, ActivityContext}
import payloads.UpdateLineItemsPayload
import responses.{CreditCardsResponse, Addresses, GiftCardResponse, CustomerResponse, FullOrder, StoreAdminResponse,
StoreCreditResponse}
import services.LineItemUpdater.foldQuantityPayload
import services.activity._
import utils.Slick.DbResult

import StoreAdminResponse.{build ⇒ buildAdmin}
import CustomerResponse.{build ⇒ buildCustomer}
import CreditCardsResponse.{buildSimple ⇒ buildCc}

object LogActivity {

  /* Assignments */
  def assignedToOrder(admin: StoreAdmin, order: FullOrder.Root, assignees: Seq[StoreAdminResponse.Root])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = {
    Activities.log(AssignedToOrder(buildAdmin(admin), order, assignees))
  }

  def unassignedFromOrder(admin: StoreAdmin, order: FullOrder.Root, assignee: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = {
    Activities.log(UnassignedFromOrder(buildAdmin(admin), order, buildAdmin(assignee)))
  }

  def bulkAssignedToOrders(admin: StoreAdmin, assignee: Option[StoreAdmin], assigneeId: Int, orders: Seq[String])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = assignee match {
    case Some(a) ⇒
      Activities.log(BulkAssignedToOrders(buildAdmin(admin), buildAdmin(a), orders))
    case _ ⇒
      DbResult.failure(NotFoundFailure404(StoreAdmin, assigneeId))
  }

  def bulkUnassignedFromOrders(admin: StoreAdmin, assignee: Option[StoreAdmin], assigneeId: Int, orders: Seq[String])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = assignee match {
    case Some(a) ⇒
      Activities.log(BulkUnassignedFromOrders(buildAdmin(admin), buildAdmin(a), orders))
    case _ ⇒
      DbResult.failure(NotFoundFailure404(StoreAdmin, assigneeId))
  }

  /* Watchers */
  def addedWatchersToOrder(admin: StoreAdmin, order: FullOrder.Root, watchers: Seq[StoreAdminResponse.Root])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = {
    Activities.log(AddedWatchersToOrder(buildAdmin(admin), order, watchers))
  }

  def removedWatcherFromOrder(admin: StoreAdmin, order: FullOrder.Root, watcher: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = {
    Activities.log(RemovedWatcherFromOrder(buildAdmin(admin), order, buildAdmin(watcher)))
  }

  def bulkAddedWatcherToOrders(admin: StoreAdmin, assignee: Option[StoreAdmin], watcherId: Int, orders: Seq[String])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = assignee match {
    case Some(a) ⇒
      Activities.log(BulkAddedWatcherToOrders(buildAdmin(admin), buildAdmin(a), orders))
    case _ ⇒
      DbResult.failure(NotFoundFailure404(StoreAdmin, watcherId))
  }

  def bulkRemovedWatcherFromOrders(admin: StoreAdmin, assignee: Option[StoreAdmin], watcherId: Int, orders: Seq[String])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = assignee match {
    case Some(a) ⇒
      Activities.log(BulkRemovedWatcherFromOrders(buildAdmin(admin), buildAdmin(a), orders))
    case _ ⇒
      DbResult.failure(NotFoundFailure404(StoreAdmin, watcherId))
  }

  /* Customers */
  def customerCreated(customer: CustomerResponse.Root, admin: Option[StoreAdmin])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = admin match {
      case Some(a) ⇒
        Activities.log(CustomerCreated(buildAdmin(a), customer))
      case _ ⇒
        Activities.log(CustomerRegistered(customer))
    }

  def customerActivated(customer: CustomerResponse.Root, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CustomerActivated(buildAdmin(admin), customer))

  def customerUpdated(customer: Customer, updated: Customer, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CustomerUpdated(buildAdmin(admin), buildCustomer(customer),
      buildCustomer(updated)))

  def customerDisabled(disabled: Boolean, customer: Customer, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = {

    val adminResponse = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (disabled) {
      Activities.log(CustomerDisabled(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerEnabled(adminResponse, customerResponse))
    }
  }

  def customerBlacklisted(blacklisted: Boolean, customer: Customer, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = {

    val adminResponse = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (blacklisted) {
      Activities.log(CustomerBlacklisted(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerRemovedFromBlacklist(adminResponse, customerResponse))
    }
  }

  /* Customer Addresses */
  def addressCreated(admin: Option[StoreAdmin], customer: Customer, address: Address, region: Region)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] = {

    val customerResponse  = buildCustomer(customer)
    val addressResponse   = Addresses.build(address, region)

    admin match {
      case Some(a) ⇒
        Activities.log(CustomerAddressCreatedByAdmin(StoreAdminResponse.build(a), customerResponse, addressResponse))
      case _ ⇒
        Activities.log(CustomerAddressCreated(customerResponse, addressResponse))
    }
  }

  def addressUpdated(admin: StoreAdmin, customer: Customer, newAddress: Address, newRegion: Region,
    oldAddress: Address, oldRegion: Region)(implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CustomerAddressUpdated(buildAdmin(admin), buildCustomer(customer),
      Addresses.build(newAddress, newRegion), Addresses.build(oldAddress, oldRegion)))


  def addressDeleted(admin: StoreAdmin, customer: Customer, address: Address, region: Region)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CustomerAddressDeleted(buildAdmin(admin), buildCustomer(customer),
      Addresses.build(address, region)))


  /* Credit Cards */
  def ccCreated(admin: StoreAdmin, customer: Customer, cc: CreditCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CreditCardAdded(buildAdmin(admin), buildCustomer(customer), buildCc(cc)))

  def ccUpdated(admin: StoreAdmin, customer: Customer, newCc: CreditCard, oldCc: CreditCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CreditCardUpdated(buildAdmin(admin), buildCustomer(customer), buildCc(newCc), buildCc(oldCc)))

  def ccDeleted(admin: StoreAdmin, customer: Customer, cc: CreditCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CreditCardRemoved(buildAdmin(admin), buildCustomer(customer), buildCc(cc)))

  /* Gift Cards */
  def gcCreated(admin: StoreAdmin, giftCard: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(GiftCardCreated(buildAdmin(admin), GiftCardResponse.build(giftCard)))

  def gcUpdated(admin: StoreAdmin, giftCard: GiftCard, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(GiftCardStateChanged(buildAdmin(admin), GiftCardResponse.build(giftCard), payload))

  def gcConvertedToSc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(GiftCardConvertedToStoreCredit(buildAdmin(admin), GiftCardResponse.build(gc),
      StoreCreditResponse.build(sc)))

  def gcFundsAuthorized(orderRefNum: String, amount: Int)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(GiftCardAuthorizedFunds(orderRefNum, amount))

  /* Store Credits */
  def scCreated(admin: StoreAdmin, customer: Customer, sc: StoreCredit)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(StoreCreditCreated(buildAdmin(admin), buildCustomer(customer),
      StoreCreditResponse.build(sc)))

  def scUpdated(admin: StoreAdmin, sc: StoreCredit, payload: payloads.StoreCreditUpdateStatusByCsr)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(StoreCreditStateChanged(buildAdmin(admin), StoreCreditResponse.build(sc), payload))

  def scConvertedToGc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(StoreCreditConvertedToGiftCard(buildAdmin(admin), GiftCardResponse.build(gc),
      StoreCreditResponse.build(sc)))

  def scFundsAuthorized(orderRefNum: String, amount: Int)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(StoreCreditAuthorizedFunds(orderRefNum, amount))

  /* Orders */
  def cartCreated(admin: StoreAdmin, order: FullOrder.Root)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(CartCreated(buildAdmin(admin), order))

  def orderStateChanged(admin: StoreAdmin, order: FullOrder.Root, oldState: Order.Status)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderStateChanged(buildAdmin(admin), order, oldState))

  def orderBulkStateChanged(admin: StoreAdmin, newState: Order.Status, orders: Seq[String])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderBulkStateChanged(buildAdmin(admin), newState, orders))

  /* Order Notes */
  def orderNoteCreated(admin: StoreAdmin, orderRefNum: String, status: Order.Status, text: String)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderNoteCreated(buildAdmin(admin), orderRefNum, status, text))

  def orderNoteUpdated(admin: StoreAdmin, orderRefNum: String, status: Order.Status, oldText: String, newText: String)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderNoteUpdated(buildAdmin(admin), orderRefNum, status, oldText, newText))

  def orderNoteDeleted(admin: StoreAdmin, orderRefNum: String, status: Order.Status, text: String)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderNoteDeleted(buildAdmin(admin), orderRefNum, status, text))

  /* Order Line Items */
  def orderLineItemsAddedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderLineItemsAddedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdatedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderLineItemsUpdatedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsDeletedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderLineItemsDeletedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdated(admin: StoreAdmin, order: FullOrder.Root, oldQtys: Map[String, Int],
    payload: Seq[UpdateLineItemsPayload])(implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderLineItemsUpdatedQuantities(buildAdmin(admin), order, oldQtys,
      foldQuantityPayload(payload)))

  def orderLineItemsUpdatedByCustomer(customer: Customer, order: FullOrder.Root, oldQtys: Map[String, Int],
    payload: Seq[UpdateLineItemsPayload])(implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderLineItemsUpdatedQuantitiesByCustomer(buildCustomer(customer), order, oldQtys,
      foldQuantityPayload(payload)))

  /* Order Payment Methods */
  def orderPaymentMethodAddedCc(admin: StoreAdmin, order: FullOrder.Root, cc: CreditCard, region: Region)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedCreditCard(buildAdmin(admin), order, CreditCardsResponse.build(cc, region)))

  def orderPaymentMethodAddedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard, amount: Int)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc), amount))

  def orderPaymentMethodAddedSc(admin: StoreAdmin, order: FullOrder.Root, amount: Int)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedStoreCredit(buildAdmin(admin), order, amount))

  def orderPaymentMethodDeleted(admin: StoreAdmin, order: FullOrder.Root, pmt: PaymentMethod.Type)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderPaymentMethodDeleted(buildAdmin(admin), order, pmt))

  def orderPaymentMethodDeletedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderPaymentMethodDeletedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  /* Order Shipping Addresses */
  def orderShippingAddressAdded(admin: StoreAdmin, order: FullOrder.Root, address: OrderShippingAddress)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderShippingAddressAdded(buildAdmin(admin), order, address))

  def orderShippingAddressUpdated(admin: StoreAdmin, order: FullOrder.Root, address: OrderShippingAddress)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderShippingAddressUpdated(buildAdmin(admin), order, address))

  def orderShippingAddressDeleted(admin: StoreAdmin, order: FullOrder.Root)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderShippingAddressRemoved(buildAdmin(admin), order))

  /* Order Shipping Methods */
  def orderShippingMethodUpdated(admin: StoreAdmin, order: FullOrder.Root, shippingMethod: Option[ShippingMethod])
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderShippingMethodUpdated(buildAdmin(admin), order, shippingMethod))

  def orderShippingMethodDeleted(admin: StoreAdmin, order: FullOrder.Root, shippingMethod: ShippingMethod)
    (implicit ec: ExecutionContext, ac: ActivityContext): DbResult[Activity] =
    Activities.log(OrderShippingMethodRemoved(buildAdmin(admin), order, shippingMethod))
}