package services

import java.time.Instant

import models.customer.Customer
import models.location.{Address, Region}
import models.order.Order
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCard
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.sharedsearch.SharedSearch
import models.shipping.ShippingMethod
import models.{StoreAdmin, Note}
import models.activity.{Activity, Activities}
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import payloads.UpdateLineItemsPayload
import responses.order.FullOrder
import responses.{CreditCardsResponse, Addresses, GiftCardResponse, CustomerResponse, StoreAdminResponse,
StoreCreditResponse}
import services.LineItemUpdater.foldQuantityPayload
import utils.Slick.DbResult
import utils.aliases._

import services.activity.AssignmentsTailored._
import services.activity.CustomerTailored._
import services.activity.GiftCardTailored._
import services.activity.OrderTailored._
import services.activity.NotesTailored._
import services.activity.SharedSearchTailored._
import services.activity.StoreCreditTailored._

import StoreAdminResponse.{build ⇒ buildAdmin, Root ⇒ AdminResponse}
import CustomerResponse.{build ⇒ buildCustomer, Root ⇒ CustomerResponse}
import CreditCardsResponse.{buildSimple ⇒ buildCc}

object LogActivity {

  /* Notes */
  def noteCreated[T](admin: StoreAdmin, entity: T, note: Note)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(NoteCreated[T](buildAdmin(admin), entity, note))

  def noteUpdated[T](admin: StoreAdmin, entity: T, oldNote: Note, note: Note)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(NoteUpdated[T](buildAdmin(admin), entity, oldNote, note))

  def noteDeleted[T](admin: StoreAdmin, entity: T, note: Note)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(NoteDeleted[T](buildAdmin(admin), entity, note))

  /* Shared Search Associations */
  def associatedWithSearch(admin: StoreAdmin, search: SharedSearch, associates: Seq[StoreAdmin])
    (implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(AssociatedWithSearch(buildAdmin(admin), search, associates.map(buildAdmin)))
  }

  def unassociatedFromSearch(admin: StoreAdmin, search: SharedSearch, associate: StoreAdmin)
    (implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(UnassociatedFromSearch(buildAdmin(admin), search, buildAdmin(associate)))
  }

  /* Order Assignments */
  def assignedToOrder(admin: StoreAdmin, order: FullOrder.Root, assignees: Seq[AdminResponse])
    (implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(AssignedToOrder(buildAdmin(admin), order, assignees))
  }

  def unassignedFromOrder(admin: StoreAdmin, order: FullOrder.Root, assignee: StoreAdmin)
    (implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(UnassignedFromOrder(buildAdmin(admin), order, buildAdmin(assignee)))
  }

  def bulkAssignedToOrders(admin: StoreAdmin, assignee: StoreAdmin, orderRefNums: Seq[String])
    (implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(BulkAssignedToOrders(buildAdmin(admin), buildAdmin(assignee), orderRefNums))
  }

  def bulkUnassignedFromOrders(admin: StoreAdmin, assignee: StoreAdmin, orderRefNums: Seq[String])
    (implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(BulkUnassignedFromOrders(buildAdmin(admin), buildAdmin(assignee), orderRefNums))
  }

  /* Customers */
  def customerCreated(customer: CustomerResponse, admin: Option[StoreAdmin])
    (implicit ec: EC, ac: AC): DbResult[Activity] = admin match {
      case Some(a) ⇒
        Activities.log(CustomerCreated(buildAdmin(a), customer))
      case _ ⇒
        Activities.log(CustomerRegistered(customer))
    }

  def customerActivated(customer: CustomerResponse, admin: StoreAdmin)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CustomerActivated(buildAdmin(admin), customer))

  def customerUpdated(customer: Customer, updated: Customer, admin: Option[StoreAdmin])
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CustomerUpdated(buildCustomer(customer), buildCustomer(updated), admin.map(buildAdmin)))

  def customerDisabled(disabled: Boolean, customer: Customer, admin: StoreAdmin)
    (implicit ec: EC, ac: AC): DbResult[Activity] = {

    val adminResponse = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (disabled) {
      Activities.log(CustomerDisabled(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerEnabled(adminResponse, customerResponse))
    }
  }

  def customerBlacklisted(blacklisted: Boolean, customer: Customer, admin: StoreAdmin)
    (implicit ec: EC, ac: AC): DbResult[Activity] = {

    val adminResponse = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (blacklisted) {
      Activities.log(CustomerBlacklisted(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerRemovedFromBlacklist(adminResponse, customerResponse))
    }
  }

  /* Customer Addresses */
  def addressCreated(originator: Originator, customer: Customer, address: Address, region: Region)
    (implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(CustomerAddressCreated(buildCustomer(customer), Addresses.build(address, region), 
      buildOriginator(originator)))
  }

  def addressUpdated(originator: Originator, customer: Customer, newAddress: Address, newRegion: Region,
    oldAddress: Address, oldRegion: Region)(implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CustomerAddressUpdated(buildCustomer(customer), Addresses.build(newAddress, newRegion),
      Addresses.build(oldAddress, oldRegion), buildOriginator(originator)))

  def addressDeleted(originator: Originator, customer: Customer, address: Address, region: Region)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CustomerAddressDeleted(buildCustomer(customer), Addresses.build(address, region),
      buildOriginator(originator)))

  /* Customer Credit Cards */
  def ccCreated(customer: Customer, cc: CreditCard, admin: Option[StoreAdmin])
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CreditCardAdded(buildCustomer(customer), buildCc(cc), admin.map(buildAdmin)))

  def ccUpdated(customer: Customer, newCc: CreditCard, oldCc: CreditCard, admin: Option[StoreAdmin])
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CreditCardUpdated(buildCustomer(customer), buildCc(newCc), buildCc(oldCc), admin.map(buildAdmin)))

  def ccDeleted(customer: Customer, cc: CreditCard, admin: Option[StoreAdmin])
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CreditCardRemoved(buildCustomer(customer), buildCc(cc), admin.map(buildAdmin)))

  /* Gift Cards */
  def gcCreated(admin: StoreAdmin, giftCard: GiftCard)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(GiftCardCreated(buildAdmin(admin), GiftCardResponse.build(giftCard)))

  def gcUpdated(admin: StoreAdmin, giftCard: GiftCard, payload: payloads.GiftCardUpdateStateByCsr)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(GiftCardStateChanged(buildAdmin(admin), GiftCardResponse.build(giftCard), payload))

  def gcConvertedToSc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(GiftCardConvertedToStoreCredit(buildAdmin(admin), GiftCardResponse.build(gc),
      StoreCreditResponse.build(sc)))

  def gcFundsAuthorized(customer: Customer, order: Order, gcCodes: Seq[String], amount: Int)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(GiftCardAuthorizedFunds(buildCustomer(customer), order, gcCodes, amount))

  /* Store Credits */
  def scCreated(admin: StoreAdmin, customer: Customer, sc: StoreCredit)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(StoreCreditCreated(buildAdmin(admin), buildCustomer(customer),
      StoreCreditResponse.build(sc)))

  def scUpdated(admin: StoreAdmin, sc: StoreCredit, payload: payloads.StoreCreditUpdateStateByCsr)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(StoreCreditStateChanged(buildAdmin(admin), StoreCreditResponse.build(sc), payload))

  def scConvertedToGc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(StoreCreditConvertedToGiftCard(buildAdmin(admin), GiftCardResponse.build(gc),
      StoreCreditResponse.build(sc)))

  def scFundsAuthorized(customer: Customer, order: Order, scIds: Seq[Int], amount: Int)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(StoreCreditAuthorizedFunds(buildCustomer(customer), order, scIds, amount))

  /* Orders */
  def cartCreated(admin: Option[StoreAdmin], order: FullOrder.Root)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CartCreated(admin.map(buildAdmin), order))

  def orderStateChanged(admin: StoreAdmin, order: FullOrder.Root, oldState: Order.State)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderStateChanged(buildAdmin(admin), order, oldState))

  def orderBulkStateChanged(admin: StoreAdmin, newState: Order.State, orderRefNums: Seq[String])
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderBulkStateChanged(buildAdmin(admin), orderRefNums, newState))

  def orderRemorsePeriodIncreased(admin: StoreAdmin, order: FullOrder.Root, oldPeriodEnd: Option[Instant])
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderRemorsePeriodIncreased(buildAdmin(admin), order, oldPeriodEnd))

  /* Order Line Items */
  def orderLineItemsAddedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderLineItemsAddedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdatedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderLineItemsUpdatedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsDeletedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderLineItemsDeletedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdated(order: FullOrder.Root, oldQtys: Map[String, Int], payload: Seq[UpdateLineItemsPayload],
    admin: Option[StoreAdmin] = None)(implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderLineItemsUpdatedQuantities(order, oldQtys, foldQuantityPayload(payload), admin.map(buildAdmin)))

  /* Order Payment Methods */
  def orderPaymentMethodAddedCc(originator: Originator, order: FullOrder.Root, cc: CreditCard, region: Region)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedCreditCard(order, CreditCardsResponse.build(cc, region), buildOriginator(originator)))

  def orderPaymentMethodAddedGc(originator: Originator, order: FullOrder.Root, gc: GiftCard, amount: Int)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedGiftCard(order, GiftCardResponse.build(gc), amount, buildOriginator(originator)))

  def orderPaymentMethodAddedSc(originator: Originator, order: FullOrder.Root, amount: Int)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedStoreCredit(order, amount, buildOriginator(originator)))

  def orderPaymentMethodDeleted(originator: Originator, order: FullOrder.Root, pmt: PaymentMethod.Type)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodDeleted(order, pmt, buildOriginator(originator)))

  def orderPaymentMethodDeletedGc(originator: Originator, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodDeletedGiftCard(order, GiftCardResponse.build(gc), buildOriginator(originator)))

  /* Order Shipping Addresses */
  def orderShippingAddressAdded(originator: Originator, order: FullOrder.Root, address: Addresses.Root)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingAddressAdded(order, address, buildOriginator(originator)))

  def orderShippingAddressUpdated(originator: Originator, order: FullOrder.Root, address: Addresses.Root)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingAddressUpdated(order, address, buildOriginator(originator)))

  def orderShippingAddressDeleted(originator: Originator, order: FullOrder.Root, address: Addresses.Root)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingAddressRemoved(order, address, buildOriginator(originator)))

  /* Order Shipping Methods */
  def orderShippingMethodUpdated(originator: Originator, order: FullOrder.Root, shippingMethod: Option[ShippingMethod])
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingMethodUpdated(order, shippingMethod, buildOriginator(originator)))

  def orderShippingMethodDeleted(originator: Originator, order: FullOrder.Root, shippingMethod: ShippingMethod)
    (implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingMethodRemoved(order, shippingMethod, buildOriginator(originator)))

  /* Helpers */
  private def buildOriginator(originator: Originator)
    (implicit ec: EC, ac: AC): Option[AdminResponse] = originator match {
    case AdminOriginator(admin)   ⇒ Some(buildAdmin(admin))
    case CustomerOriginator(_)    ⇒ None // We don't need customer, he's already in FullOrder.Root / Customer object
  }
}
