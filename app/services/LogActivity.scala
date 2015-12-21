package services

import scala.concurrent.ExecutionContext

import models.{PaymentMethod, StoreCredit, GiftCard, CreditCard, OrderShippingMethod, OrderShippingAddress, Region,
Address, Customer, StoreAdmin}
import payloads.UpdateLineItemsPayload
import responses.{CreditCardsResponse, Addresses, GiftCardResponse, CustomerResponse, FullOrder, StoreAdminResponse,
StoreCreditResponse}
import services.activity._
import CreditCardManager.{buildResponse ⇒ buildCc}
import StoreAdminResponse.{build ⇒ buildAdmin}
import CustomerResponse.{build ⇒ buildCustomer}
import models.activity.{Activities, ActivityContext}

object LogActivity {

  /* Customers */
  def customerCreated(customer: CustomerResponse.Root, admin: Option[StoreAdmin])
    (implicit ec: ExecutionContext, ac: ActivityContext) = admin match {
      case Some(a) ⇒
        Activities.log(CustomerCreated(buildAdmin(a), customer))
      case _ ⇒
        Activities.log(CustomerRegistered(customer))
    }

  def customerActivated(customer: CustomerResponse.Root, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(CustomerActivated(buildAdmin(admin), customer))

  def customerUpdated(customer: Customer, updated: Customer, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(CustomerUpdated(buildAdmin(admin), buildCustomer(customer),
      buildCustomer(updated)))

  def customerDisabled(disabled: Boolean, customer: Customer, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext) = {

    val adminResponse = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (disabled) {
      Activities.log(CustomerDisabled(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerEnabled(adminResponse, customerResponse))
    }
  }

  def customerBlacklisted(blacklisted: Boolean, customer: Customer, admin: StoreAdmin)
    (implicit ec: ExecutionContext, ac: ActivityContext) = {

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
    (implicit ec: ExecutionContext, ac: ActivityContext) = {

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
    oldAddress: Address, oldRegion: Region)(implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(CustomerAddressUpdated(buildAdmin(admin), buildCustomer(customer),
      Addresses.build(newAddress, newRegion), Addresses.build(oldAddress, oldRegion)))


  def addressDeleted(admin: StoreAdmin, customer: Customer, address: Address, region: Region)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(CustomerAddressDeleted(buildAdmin(admin), buildCustomer(customer),
      Addresses.build(address, region)))


  /* Credit Cards */
  def ccCreated(admin: StoreAdmin, customer: Customer, cc: CreditCard, region: Region)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(CreditCardAdded(buildAdmin(admin), buildCustomer(customer), buildCc(cc, region)))

  def ccUpdated(admin: StoreAdmin, customer: Customer, newCc: CreditCard, newRegion: Region, oldCc:
    CreditCard, oldRegion: Region)(implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(CreditCardUpdated(buildAdmin(admin), buildCustomer(customer),
      buildCc(oldCc, oldRegion), buildCc(newCc, newRegion)))

  def ccDeleted(admin: StoreAdmin, customer: Customer, cc: CreditCard, region: Region)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(CreditCardRemoved(buildAdmin(admin), buildCustomer(customer),
      buildCc(cc, region)))

  /* Gift Cards */
  def gcCreated(admin: StoreAdmin, giftCard: GiftCard)(implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(GiftCardCreated(buildAdmin(admin), GiftCardResponse.build(giftCard)))

  def gcUpdated(admin: StoreAdmin, giftCard: GiftCard, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(GiftCardStateChanged(buildAdmin(admin), GiftCardResponse.build(giftCard), payload))

  def gcConvertedToSc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(GiftCardConvertedToStoreCredit(buildAdmin(admin), GiftCardResponse.build(gc),
      StoreCreditResponse.build(sc)))

  /* Store Credits */
  def scCreated(admin: StoreAdmin, customer: Customer, sc: StoreCredit)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(StoreCreditCreated(buildAdmin(admin), buildCustomer(customer),
      StoreCreditResponse.build(sc)))

  def scUpdated(admin: StoreAdmin, sc: StoreCredit, payload: payloads.StoreCreditUpdateStatusByCsr)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(StoreCreditStateChanged(buildAdmin(admin), StoreCreditResponse.build(sc), payload))

  def scConvertedToGc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(StoreCreditConvertedToGiftCard(buildAdmin(admin), GiftCardResponse.build(gc),
      StoreCreditResponse.build(sc)))

  /* Order Line Items */
  def orderLineItemsAddedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderLineItemsAddedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdatedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderLineItemsUpdatedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsDeletedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderLineItemsDeletedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdated(admin: StoreAdmin, order: FullOrder.Root, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderLineItemsUpdatedQuantities(buildAdmin(admin), order, payload))

  def orderLineItemsUpdatedByCustomer(customer: Customer, order: FullOrder.Root, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderLineItemsUpdatedQuantitiesByCustomer(buildCustomer(customer), order, payload))

  /* Order Payment Methods */
  def orderPaymentMethodAddedCc(admin: StoreAdmin, order: FullOrder.Root, cc: CreditCard, region: Region)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderPaymentMethodAddedCreditCard(buildAdmin(admin), order, CreditCardsResponse.build(cc, region)))

  def orderPaymentMethodAddedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard, amount: Int)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderPaymentMethodAddedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc), amount))

  def orderPaymentMethodAddedSc(admin: StoreAdmin, order: FullOrder.Root, amount: Int)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderPaymentMethodAddedStoreCredit(buildAdmin(admin), order, amount))

  def orderPaymentMethodDeleted(admin: StoreAdmin, order: FullOrder.Root, pmt: PaymentMethod.Type)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderPaymentMethodDeleted(buildAdmin(admin), order, pmt))

  def orderPaymentMethodDeletedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderPaymentMethodDeletedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  /* Order Shipping Addresses */
  def orderShippingAddressAdded(admin: StoreAdmin, order: FullOrder.Root, address: OrderShippingAddress)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderShippingAddressAdded(buildAdmin(admin), order, address))

  def orderShippingAddressUpdated(admin: StoreAdmin, order: FullOrder.Root, address: OrderShippingAddress)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderShippingAddressUpdated(buildAdmin(admin), order, address))

  def orderShippingAddressDeleted(admin: StoreAdmin, order: FullOrder.Root)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderShippingAddressRemoved(buildAdmin(admin), order))

  /* Order Shipping Methods */
  def orderShippingMethodUpdated(admin: StoreAdmin, order: FullOrder.Root, method: OrderShippingMethod)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderShippingMethodUpdated(buildAdmin(admin), order, method))

  def orderShippingMethodDeleted(admin: StoreAdmin, order: FullOrder.Root)
    (implicit ec: ExecutionContext, ac: ActivityContext) =
    Activities.log(OrderShippingMethodRemoved(buildAdmin(admin), order))
}