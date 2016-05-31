package services

import java.time.Instant

import models.Assignment._
import models.customer.Customer
import models.location.{Address, Region}
import models.order.Order
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCardCharge}
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.sharedsearch.SharedSearch
import models.shipping.ShippingMethod
import models.{Note, StoreAdmin}
import models.activity.{Activities, Activity}
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import responses.CategoryResponses.FullCategoryResponse
import responses.order.FullOrder
import responses.{Addresses, CreditCardsResponse, CustomerResponse, GiftCardResponse, StoreAdminResponse, StoreCreditResponse}
import services.LineItemUpdater.foldQuantityPayload
import services.activity.CategoryTailored.{FullCategoryCreated, FullCategoryUpdated}
import utils.aliases._
import utils.db._
import services.activity.AssignmentsTailored._
import services.activity.CustomerTailored._
import services.activity.GiftCardTailored._
import services.activity.OrderTailored._
import services.activity.NotesTailored._
import services.activity.SharedSearchTailored._
import services.activity.StoreCreditTailored._
import services.activity.CouponsTailored._
import StoreAdminResponse.{Root ⇒ AdminResponse, build ⇒ buildAdmin, _}
import CustomerResponse.{Root ⇒ CustomerResponse, build ⇒ buildCustomer}
import CreditCardsResponse.{buildSimple ⇒ buildCc}
import models.coupon.{Coupon, CouponCode}
import payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import responses.CouponResponses.CouponResponse
import responses.ProductResponses.FullProductResponse
import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses.FullSkuResponse
import services.activity.ProductTailored.{FullProductCreated, FullProductUpdated}
import services.activity.SkuTailored.{FullSkuCreated, FullSkuUpdated}

object LogActivity {

  /* Assignments */
  def assigned[T](admin: StoreAdmin,
                  entity: T,
                  assignees: Seq[AdminResponse],
                  assignType: AssignmentType,
                  refType: ReferenceType)(implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(Assigned[T](buildAdmin(admin), entity, assignees, assignType, refType))
  }

  def unassigned[T](admin: StoreAdmin,
                    entity: T,
                    assignee: StoreAdmin,
                    assignType: AssignmentType,
                    refType: ReferenceType)(implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(
        Unassigned[T](buildAdmin(admin), entity, buildAdmin(assignee), assignType, refType))
  }

  def bulkAssigned(admin: StoreAdmin,
                   assignee: StoreAdmin,
                   entityIds: Seq[String],
                   assignType: AssignmentType,
                   refType: ReferenceType)(implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(
        BulkAssigned(buildAdmin(admin), buildAdmin(assignee), entityIds, assignType, refType))
  }

  def bulkUnassigned(admin: StoreAdmin,
                     assignee: StoreAdmin,
                     entityIds: Seq[String],
                     assignType: AssignmentType,
                     refType: ReferenceType)(implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(
        BulkUnassigned(buildAdmin(admin), buildAdmin(assignee), entityIds, assignType, refType))
  }

  /* Notes */
  def noteCreated[T](admin: StoreAdmin, entity: T, note: Note)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(NoteCreated[T](buildAdmin(admin), entity, note))

  def noteUpdated[T](admin: StoreAdmin, entity: T, oldNote: Note, note: Note)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(NoteUpdated[T](buildAdmin(admin), entity, oldNote, note))

  def noteDeleted[T](admin: StoreAdmin, entity: T, note: Note)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(NoteDeleted[T](buildAdmin(admin), entity, note))

  /* Shared Search Associations */
  def associatedWithSearch(admin: StoreAdmin, search: SharedSearch, associates: Seq[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(AssociatedWithSearch(buildAdmin(admin), search, associates.map(buildAdmin)))
  }

  def unassociatedFromSearch(admin: StoreAdmin, search: SharedSearch, associate: StoreAdmin)(
      implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(UnassociatedFromSearch(buildAdmin(admin), search, buildAdmin(associate)))
  }

  /* Customers */
  def customerCreated(customer: CustomerResponse, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] = admin match {
    case Some(a) ⇒
      Activities.log(CustomerCreated(buildAdmin(a), customer))
    case _ ⇒
      Activities.log(CustomerRegistered(customer))
  }

  def customerActivated(customer: CustomerResponse, admin: StoreAdmin)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CustomerActivated(buildAdmin(admin), customer))

  def customerUpdated(customer: Customer, updated: Customer, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        CustomerUpdated(buildCustomer(customer), buildCustomer(updated), admin.map(buildAdmin)))

  def customerDisabled(disabled: Boolean, customer: Customer, admin: StoreAdmin)(
      implicit ec: EC, ac: AC): DbResult[Activity] = {

    val adminResponse    = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (disabled) {
      Activities.log(CustomerDisabled(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerEnabled(adminResponse, customerResponse))
    }
  }

  def customerBlacklisted(blacklisted: Boolean, customer: Customer, admin: StoreAdmin)(
      implicit ec: EC, ac: AC): DbResult[Activity] = {

    val adminResponse    = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (blacklisted) {
      Activities.log(CustomerBlacklisted(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerRemovedFromBlacklist(adminResponse, customerResponse))
    }
  }

  /* Customer Addresses */
  def addressCreated(originator: Originator, customer: Customer, address: Address, region: Region)(
      implicit ec: EC, ac: AC): DbResult[Activity] = {
    Activities.log(
        CustomerAddressCreated(buildCustomer(customer),
                               Addresses.build(address, region),
                               buildOriginator(originator)))
  }

  def addressUpdated(originator: Originator,
                     customer: Customer,
                     newAddress: Address,
                     newRegion: Region,
                     oldAddress: Address,
                     oldRegion: Region)(implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        CustomerAddressUpdated(buildCustomer(customer),
                               Addresses.build(newAddress, newRegion),
                               Addresses.build(oldAddress, oldRegion),
                               buildOriginator(originator)))

  def addressDeleted(originator: Originator, customer: Customer, address: Address, region: Region)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        CustomerAddressDeleted(buildCustomer(customer),
                               Addresses.build(address, region),
                               buildOriginator(originator)))

  /* Customer Credit Cards */
  def ccCreated(customer: Customer, cc: CreditCard, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CreditCardAdded(buildCustomer(customer), buildCc(cc), admin.map(buildAdmin)))

  def ccUpdated(
      customer: Customer, newCc: CreditCard, oldCc: CreditCard, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CreditCardUpdated(
            buildCustomer(customer), buildCc(newCc), buildCc(oldCc), admin.map(buildAdmin)))

  def ccDeleted(customer: Customer, cc: CreditCard, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CreditCardRemoved(buildCustomer(customer), buildCc(cc), admin.map(buildAdmin)))

  /* Gift Cards */
  def gcCreated(admin: StoreAdmin, giftCard: GiftCard)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(GiftCardCreated(buildAdmin(admin), GiftCardResponse.build(giftCard)))

  def gcUpdated(admin: StoreAdmin, giftCard: GiftCard, payload: GiftCardUpdateStateByCsr)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        GiftCardStateChanged(buildAdmin(admin), GiftCardResponse.build(giftCard), payload))

  def gcConvertedToSc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(GiftCardConvertedToStoreCredit(
            buildAdmin(admin), GiftCardResponse.build(gc), StoreCreditResponse.build(sc)))

  def gcFundsAuthorized(customer: Customer, order: Order, gcCodes: Seq[String], amount: Int)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(GiftCardAuthorizedFunds(buildCustomer(customer), order, gcCodes, amount))

  /* Store Credits */
  def scCreated(admin: StoreAdmin, customer: Customer, sc: StoreCredit)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(StoreCreditCreated(
            buildAdmin(admin), buildCustomer(customer), StoreCreditResponse.build(sc)))

  def scUpdated(admin: StoreAdmin, sc: StoreCredit, payload: StoreCreditUpdateStateByCsr)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        StoreCreditStateChanged(buildAdmin(admin), StoreCreditResponse.build(sc), payload))

  def scConvertedToGc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(StoreCreditConvertedToGiftCard(
            buildAdmin(admin), GiftCardResponse.build(gc), StoreCreditResponse.build(sc)))

  def scFundsAuthorized(customer: Customer, order: Order, scIds: Seq[Int], amount: Int)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(StoreCreditAuthorizedFunds(buildCustomer(customer), order, scIds, amount))

  /* Orders */
  def cartCreated(admin: Option[StoreAdmin], order: FullOrder.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CartCreated(admin.map(buildAdmin), order))

  def orderStateChanged(admin: StoreAdmin, order: FullOrder.Root, oldState: Order.State)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderStateChanged(buildAdmin(admin), order, oldState))

  def orderBulkStateChanged(admin: StoreAdmin, newState: Order.State, orderRefNums: Seq[String])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderBulkStateChanged(buildAdmin(admin), orderRefNums, newState))

  def orderRemorsePeriodIncreased(
      admin: StoreAdmin, order: FullOrder.Root, oldPeriodEnd: Option[Instant])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderRemorsePeriodIncreased(buildAdmin(admin), order, oldPeriodEnd))

  /* Order Line Items */
  def orderLineItemsAddedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        OrderLineItemsAddedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdatedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        OrderLineItemsUpdatedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsDeletedGc(admin: StoreAdmin, order: FullOrder.Root, gc: GiftCard)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        OrderLineItemsDeletedGiftCard(buildAdmin(admin), order, GiftCardResponse.build(gc)))

  def orderLineItemsUpdated(
      order: FullOrder.Root,
      oldQtys: Map[String, Int],
      payload: Seq[UpdateLineItemsPayload],
      admin: Option[StoreAdmin] = None)(implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderLineItemsUpdatedQuantities(
            order, oldQtys, foldQuantityPayload(payload), admin.map(buildAdmin)))

  /* Order checkout & payments */

  def orderCheckoutCompleted(order: FullOrder.Root)(implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderCheckoutCompleted(order))

  def creditCardCharge(
      order: Order, charge: CreditCardCharge)(implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(
        CreditCardChargeCompleted(
            customerId = order.customerId,
            orderId = order.id,
            orderNum = order.refNum,
            cardId = charge.creditCardId,
            amount = charge.amount,
            currency = charge.currency
        ))

  /* Order Payment Methods */
  def orderPaymentMethodAddedCc(
      originator: Originator, order: FullOrder.Root, cc: CreditCard, region: Region)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedCreditCard(
            order, CreditCardsResponse.build(cc, region), buildOriginator(originator)))

  def orderPaymentMethodAddedGc(
      originator: Originator, order: FullOrder.Root, gc: GiftCard, amount: Int)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedGiftCard(
            order, GiftCardResponse.build(gc), amount, buildOriginator(originator)))

  def orderPaymentMethodUpdatedGc(originator: Originator,
                                  order: FullOrder.Root,
                                  gc: GiftCard,
                                  oldAmount: Option[Int],
                                  amount: Int)(implicit ec: EC, ac: AC): DbResult[Activity] = {
    val activity = OrderPaymentMethodUpdatedGiftCard(
        order, GiftCardResponse.build(gc), oldAmount, amount, buildOriginator(originator))

    Activities.log(activity)
  }

  def orderPaymentMethodAddedSc(originator: Originator, order: FullOrder.Root, amount: Int)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodAddedStoreCredit(order, amount, buildOriginator(originator)))

  def orderPaymentMethodDeleted(
      originator: Originator, order: FullOrder.Root, pmt: PaymentMethod.Type)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodDeleted(order, pmt, buildOriginator(originator)))

  def orderPaymentMethodDeletedGc(originator: Originator, order: FullOrder.Root, gc: GiftCard)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderPaymentMethodDeletedGiftCard(
            order, GiftCardResponse.build(gc), buildOriginator(originator)))

  /* Order Shipping Addresses */
  def orderShippingAddressAdded(
      originator: Originator, order: FullOrder.Root, address: Addresses.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingAddressAdded(order, address, buildOriginator(originator)))

  def orderShippingAddressUpdated(
      originator: Originator, order: FullOrder.Root, address: Addresses.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingAddressUpdated(order, address, buildOriginator(originator)))

  def orderShippingAddressDeleted(
      originator: Originator, order: FullOrder.Root, address: Addresses.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingAddressRemoved(order, address, buildOriginator(originator)))

  /* Order Shipping Methods */
  def orderShippingMethodUpdated(
      originator: Originator, order: FullOrder.Root, shippingMethod: Option[ShippingMethod])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingMethodUpdated(order, shippingMethod, buildOriginator(originator)))

  def orderShippingMethodDeleted(
      originator: Originator, order: FullOrder.Root, shippingMethod: ShippingMethod)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderShippingMethodRemoved(order, shippingMethod, buildOriginator(originator)))

  /* Order Coupons */
  def orderCouponAttached(order: Order, couponCode: CouponCode)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderCouponAttached(order, couponCode))

  def orderCouponDetached(order: Order)(implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(OrderCouponDetached(order))

  /* Categories */
  def fullCategoryCreated(admin: Option[StoreAdmin],
                          category: FullCategoryResponse.Root,
                          context: ObjectContextResponse.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(FullCategoryCreated(admin.map(buildAdmin), category, context))

  def fullCategoryUpdated(admin: Option[StoreAdmin],
                          category: FullCategoryResponse.Root,
                          context: ObjectContextResponse.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(FullCategoryUpdated(admin.map(buildAdmin), category, context))

  /* Products */
  def fullProductCreated(admin: Option[StoreAdmin],
                         product: FullProductResponse.Root,
                         context: ObjectContextResponse.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(FullProductCreated(admin.map(buildAdmin), product, context))

  def fullProductUpdated(admin: Option[StoreAdmin],
                         product: FullProductResponse.Root,
                         context: ObjectContextResponse.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(FullProductUpdated(admin.map(buildAdmin), product, context))

  /* SKUs */
  def fullSkuCreated(admin: Option[StoreAdmin],
                     product: FullSkuResponse.Root,
                     context: ObjectContextResponse.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(FullSkuCreated(admin.map(buildAdmin), product, context))

  def fullSkuUpdated(admin: Option[StoreAdmin],
                     product: FullSkuResponse.Root,
                     context: ObjectContextResponse.Root)(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(FullSkuUpdated(admin.map(buildAdmin), product, context))

  /* Coupons */
  def couponCreated(couponResponse: CouponResponse.Root, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CouponCreated(couponResponse, admin.map(buildAdmin(_))))

  def couponUpdated(couponResponse: CouponResponse.Root, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(CouponUpdated(couponResponse, admin.map(buildAdmin(_))))

  def singleCouponCodeCreated(coupon: Coupon, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(SingleCouponCodeGenerated(coupon, admin.map(buildAdmin(_))))

  def multipleCouponCodeCreated(coupon: Coupon, admin: Option[StoreAdmin])(
      implicit ec: EC, ac: AC): DbResult[Activity] =
    Activities.log(MultipleCouponCodesGenerated(coupon, admin.map(buildAdmin(_))))

  /* Helpers */
  private def buildOriginator(originator: Originator): Option[AdminResponse] = originator match {
    case AdminOriginator(admin) ⇒ Some(buildAdmin(admin))
    case CustomerOriginator(_) ⇒
      None // We don't need customer, he's already in FullOrder.Root / Customer object
  }
}
