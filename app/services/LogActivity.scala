package services

import java.time.Instant

import models.Assignment._
import models.activity.{Activities, Activity}
import models.cord.{Cart, Order}
import models.coupon.{Coupon, CouponCode}
import models.customer.Customer
import models.location.Region
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCardCharge}
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.sharedsearch.SharedSearch
import models.shipping.ShippingMethod
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import models.{Note, StoreAdmin}
import payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import responses.CategoryResponses.FullCategoryResponse
import responses.CouponResponses.CouponResponse
import responses.CreditCardsResponse.{buildSimple ⇒ buildCc}
import responses.CustomerResponse.{Root ⇒ CustomerResponse, build ⇒ buildCustomer}
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses.ProductResponse
import responses.SkuResponses.SkuResponse
import responses.CaptureResponse
import responses.StoreAdminResponse.{Root ⇒ AdminResponse, build ⇒ buildAdmin}
import responses.cord.{CartResponse, OrderResponse}
import responses.{AddressResponse, CreditCardsResponse, GiftCardResponse, StoreCreditResponse}
import services.LineItemUpdater.foldQuantityPayload
import services.activity.AssignmentsTailored._
import services.activity.CartTailored._
import services.activity.CategoryTailored._
import services.activity.CouponsTailored._
import services.activity.CustomerTailored._
import services.activity.GiftCardTailored._
import services.activity.MailTailored._
import services.activity.NotesTailored._
import services.activity.OrderTailored._
import services.activity.ProductTailored._
import services.activity.SharedSearchTailored._
import services.activity.SkuTailored._
import services.activity.StoreAdminsTailored._
import services.activity.StoreCreditTailored._
import utils.aliases._
import utils.db._

object LogActivity {

  /* Assignments */
  def assigned[T](admin: StoreAdmin,
                  entity: T,
                  assignees: Seq[AdminResponse],
                  assignType: AssignmentType,
                  refType: ReferenceType)(implicit ec: EC, ac: AC): DbResultT[Activity] = {
    Activities.log(Assigned[T](buildAdmin(admin), entity, assignees, assignType, refType))
  }

  def unassigned[T](admin: StoreAdmin,
                    entity: T,
                    assignee: StoreAdmin,
                    assignType: AssignmentType,
                    refType: ReferenceType)(implicit ec: EC, ac: AC): DbResultT[Activity] = {
    Activities.log(
        Unassigned[T](buildAdmin(admin), entity, buildAdmin(assignee), assignType, refType))
  }

  def bulkAssigned(admin: StoreAdmin,
                   assignee: StoreAdmin,
                   entityIds: Seq[String],
                   assignType: AssignmentType,
                   refType: ReferenceType)(implicit ec: EC, ac: AC): DbResultT[Activity] = {
    Activities.log(
        BulkAssigned(buildAdmin(admin), buildAdmin(assignee), entityIds, assignType, refType))
  }

  def bulkUnassigned(admin: StoreAdmin,
                     assignee: StoreAdmin,
                     entityIds: Seq[String],
                     assignType: AssignmentType,
                     refType: ReferenceType)(implicit ec: EC, ac: AC): DbResultT[Activity] = {
    Activities.log(
        BulkUnassigned(buildAdmin(admin), buildAdmin(assignee), entityIds, assignType, refType))
  }

  /* Notes */
  def noteCreated[T](admin: StoreAdmin, entity: T, note: Note)(implicit ec: EC,
                                                               ac: AC): DbResultT[Activity] =
    Activities.log(NoteCreated[T](buildAdmin(admin), entity, note))

  def noteUpdated[T](admin: StoreAdmin, entity: T, oldNote: Note, note: Note)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(NoteUpdated[T](buildAdmin(admin), entity, oldNote, note))

  def noteDeleted[T](admin: StoreAdmin, entity: T, note: Note)(implicit ec: EC,
                                                               ac: AC): DbResultT[Activity] =
    Activities.log(NoteDeleted[T](buildAdmin(admin), entity, note))

  /* Shared Search Associations */
  def associatedWithSearch(admin: StoreAdmin, search: SharedSearch, associates: Seq[StoreAdmin])(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] = {
    Activities.log(AssociatedWithSearch(buildAdmin(admin), search, associates.map(buildAdmin)))
  }

  def unassociatedFromSearch(admin: StoreAdmin, search: SharedSearch, associate: StoreAdmin)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] = {
    Activities.log(UnassociatedFromSearch(buildAdmin(admin), search, buildAdmin(associate)))
  }

  /* Customers */
  def customerCreated(customer: CustomerResponse,
                      admin: Option[StoreAdmin])(implicit ec: EC, ac: AC): DbResultT[Activity] =
    admin match {
      case Some(a) ⇒
        Activities.log(CustomerCreated(buildAdmin(a), customer))
      case _ ⇒
        Activities.log(CustomerRegistered(customer))
    }

  def customerActivated(customer: CustomerResponse,
                        admin: StoreAdmin)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CustomerActivated(buildAdmin(admin), customer))

  def customerUpdated(customer: Customer, updated: Customer, admin: Option[StoreAdmin])(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        CustomerUpdated(buildCustomer(customer), buildCustomer(updated), admin.map(buildAdmin)))

  def customerDisabled(disabled: Boolean, customer: Customer, admin: StoreAdmin)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] = {

    val adminResponse    = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (disabled) {
      Activities.log(CustomerDisabled(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerEnabled(adminResponse, customerResponse))
    }
  }

  def customerBlacklisted(blacklisted: Boolean, customer: Customer, admin: StoreAdmin)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] = {

    val adminResponse    = buildAdmin(admin)
    val customerResponse = buildCustomer(customer)

    if (blacklisted) {
      Activities.log(CustomerBlacklisted(adminResponse, customerResponse))
    } else {
      Activities.log(CustomerRemovedFromBlacklist(adminResponse, customerResponse))
    }
  }

  def customerRemindPassword(customer: Customer, code: String)(implicit ec: EC,
                                                               ac: AC): DbResultT[Activity] = {
    val customerResponse = buildCustomer(customer)
    Activities.log(CustomerRemindPassword(customer = customerResponse, code = code))
  }

  def customerPasswordReset(customer: Customer)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CustomerPasswordReset(customer = buildCustomer(customer)))

  /* Customer Addresses */
  def addressCreated(originator: Originator, customer: Customer, address: AddressResponse)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] = {
    Activities.log(
        CustomerAddressCreated(buildCustomer(customer), address, buildOriginator(originator)))
  }

  def addressUpdated(originator: Originator,
                     customer: Customer,
                     newAddress: AddressResponse,
                     oldAddress: AddressResponse)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(
        CustomerAddressUpdated(customer = buildCustomer(customer),
                               newInfo = newAddress,
                               oldInfo = oldAddress,
                               admin = buildOriginator(originator)))

  def addressDeleted(originator: Originator, customer: Customer, address: AddressResponse)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        CustomerAddressDeleted(buildCustomer(customer), address, buildOriginator(originator)))

  /* Customer Credit Cards */
  def ccCreated(customer: Customer, cc: CreditCard, admin: Option[StoreAdmin])(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(CreditCardAdded(buildCustomer(customer), buildCc(cc), admin.map(buildAdmin)))

  def ccUpdated(customer: Customer,
                newCc: CreditCard,
                oldCc: CreditCard,
                admin: Option[StoreAdmin])(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(
        CreditCardUpdated(buildCustomer(customer),
                          buildCc(newCc),
                          buildCc(oldCc),
                          admin.map(buildAdmin)))

  def ccDeleted(customer: Customer, cc: CreditCard, admin: Option[StoreAdmin])(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(CreditCardRemoved(buildCustomer(customer), buildCc(cc), admin.map(buildAdmin)))

  /* Gift Cards */
  def gcCreated(admin: StoreAdmin, giftCard: GiftCard)(implicit ec: EC,
                                                       ac: AC): DbResultT[Activity] =
    Activities.log(GiftCardCreated(buildAdmin(admin), GiftCardResponse.build(giftCard)))

  def gcUpdated(admin: StoreAdmin, giftCard: GiftCard, payload: GiftCardUpdateStateByCsr)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        GiftCardStateChanged(buildAdmin(admin), GiftCardResponse.build(giftCard), payload))

  def gcConvertedToSc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        GiftCardConvertedToStoreCredit(buildAdmin(admin),
                                       GiftCardResponse.build(gc),
                                       StoreCreditResponse.build(sc)))

  def gcFundsAuthorized(customer: Customer, cart: Cart, gcCodes: Seq[String], amount: Int)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(GiftCardAuthorizedFunds(buildCustomer(customer), cart, gcCodes, amount))

  def gcFundsCaptured(customer: Customer, order: Order, gcCodes: Seq[String], amount: Int)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(GiftCardCapturedFunds(buildCustomer(customer), order, gcCodes, amount))

  /* Store Credits */
  def scCreated(admin: StoreAdmin, customer: Customer, sc: StoreCredit)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        StoreCreditCreated(buildAdmin(admin),
                           buildCustomer(customer),
                           StoreCreditResponse.build(sc)))

  def scUpdated(admin: StoreAdmin, sc: StoreCredit, payload: StoreCreditUpdateStateByCsr)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        StoreCreditStateChanged(buildAdmin(admin), StoreCreditResponse.build(sc), payload))

  def scConvertedToGc(admin: StoreAdmin, gc: GiftCard, sc: StoreCredit)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        StoreCreditConvertedToGiftCard(buildAdmin(admin),
                                       GiftCardResponse.build(gc),
                                       StoreCreditResponse.build(sc)))

  def scFundsAuthorized(customer: Customer, cart: Cart, scIds: Seq[Int], amount: Int)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(StoreCreditAuthorizedFunds(buildCustomer(customer), cart, scIds, amount))

  def scFundsCaptured(customer: Customer, order: Order, scIds: Seq[Int], amount: Int)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(StoreCreditCapturedFunds(buildCustomer(customer), order, scIds, amount))

  /* Carts */
  def cartCreated(admin: Option[StoreAdmin], cart: CartResponse)(implicit ec: EC,
                                                                 ac: AC): DbResultT[Activity] =
    Activities.log(CartCreated(admin.map(buildAdmin), cart))

  /* Orders */
  def orderStateChanged(admin: StoreAdmin, order: OrderResponse, oldState: Order.State)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(OrderStateChanged(buildAdmin(admin), order, oldState))

  def orderBulkStateChanged(admin: StoreAdmin, newState: Order.State, cordRefNums: Seq[String])(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(OrderBulkStateChanged(buildAdmin(admin), cordRefNums, newState))

  def orderRemorsePeriodIncreased(
      admin: StoreAdmin,
      order: OrderResponse,
      oldPeriodEnd: Option[Instant])(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(OrderRemorsePeriodIncreased(buildAdmin(admin), order, oldPeriodEnd))

  /* Cart Line Items */
  def orderLineItemsAddedGc(admin: StoreAdmin, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(CartLineItemsAddedGiftCard(buildAdmin(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsUpdatedGc(admin: StoreAdmin, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        CartLineItemsUpdatedGiftCard(buildAdmin(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsDeletedGc(admin: StoreAdmin, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        CartLineItemsDeletedGiftCard(buildAdmin(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsUpdated(
      cart: CartResponse,
      oldQtys: Map[String, Int],
      payload: Seq[UpdateLineItemsPayload],
      admin: Option[StoreAdmin] = None)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(
        CartLineItemsUpdatedQuantities(cart,
                                       oldQtys,
                                       foldQuantityPayload(payload),
                                       admin.map(buildAdmin)))

  /* Order checkout & payments */

  def orderCheckoutCompleted(order: OrderResponse)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(OrderCheckoutCompleted(order))

  def orderCaptured(order: Order, cap: CaptureResponse)(implicit ec: EC,
                                                        ac: AC): DbResultT[Activity] =
    Activities.log(
        OrderCaptured(orderNum = order.referenceNumber,
                      customerId = order.customerId,
                      captured = cap.captured,
                      external = cap.external,
                      internal = cap.internal,
                      lineItems = cap.lineItems,
                      taxes = cap.taxes,
                      shipping = cap.shipping,
                      currency = cap.currency))

  def creditCardAuth(cart: Cart, charge: CreditCardCharge)(implicit ec: EC,
                                                           ac: AC): DbResultT[Activity] =
    Activities.log(
        CreditCardAuthCompleted(
            customerId = cart.customerId,
            cordRef = cart.refNum,
            orderNum = cart.refNum,
            cardId = charge.creditCardId,
            amount = charge.amount,
            currency = charge.currency
        ))

  def creditCardCharge(order: Order, charge: CreditCardCharge)(implicit ec: EC,
                                                               ac: AC): DbResultT[Activity] =
    Activities.log(
        CreditCardChargeCompleted(
            customerId = order.customerId,
            cordRef = order.refNum,
            orderNum = order.refNum,
            cardId = charge.creditCardId,
            amount = charge.amount,
            currency = charge.currency
        ))

  /* Cart Payment Methods */
  def orderPaymentMethodAddedCc(originator: Originator,
                                cart: CartResponse,
                                cc: CreditCard,
                                region: Region)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(
        CartPaymentMethodAddedCreditCard(cart,
                                         CreditCardsResponse.build(cc, region),
                                         buildOriginator(originator)))

  def orderPaymentMethodAddedGc(originator: Originator,
                                cart: CartResponse,
                                gc: GiftCard,
                                amount: Int)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(
        CartPaymentMethodAddedGiftCard(cart,
                                       GiftCardResponse.build(gc),
                                       amount,
                                       buildOriginator(originator)))

  def orderPaymentMethodUpdatedGc(originator: Originator,
                                  cart: CartResponse,
                                  gc: GiftCard,
                                  oldAmount: Option[Int],
                                  amount: Int)(implicit ec: EC, ac: AC): DbResultT[Activity] = {
    val activity = CartPaymentMethodUpdatedGiftCard(cart,
                                                    GiftCardResponse.build(gc),
                                                    oldAmount,
                                                    amount,
                                                    buildOriginator(originator))

    Activities.log(activity)
  }

  def orderPaymentMethodAddedSc(originator: Originator, cart: CartResponse, amount: Int)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(CartPaymentMethodAddedStoreCredit(cart, amount, buildOriginator(originator)))

  def orderPaymentMethodDeleted(
      originator: Originator,
      cart: CartResponse,
      pmt: PaymentMethod.Type)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CartPaymentMethodDeleted(cart, pmt, buildOriginator(originator)))

  def orderPaymentMethodDeletedGc(originator: Originator, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(
        CartPaymentMethodDeletedGiftCard(cart,
                                         GiftCardResponse.build(gc),
                                         buildOriginator(originator)))

  /* Cart Shipping Addresses */
  def orderShippingAddressAdded(
      originator: Originator,
      cart: CartResponse,
      address: AddressResponse)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CartShippingAddressAdded(cart, address, buildOriginator(originator)))

  def orderShippingAddressUpdated(
      originator: Originator,
      cart: CartResponse,
      address: AddressResponse)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CartShippingAddressUpdated(cart, address, buildOriginator(originator)))

  def orderShippingAddressDeleted(
      originator: Originator,
      cart: CartResponse,
      address: AddressResponse)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CartShippingAddressRemoved(cart, address, buildOriginator(originator)))

  /* Cart Shipping Methods */
  def orderShippingMethodUpdated(
      originator: Originator,
      cart: CartResponse,
      shippingMethod: Option[ShippingMethod])(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CartShippingMethodUpdated(cart, shippingMethod, buildOriginator(originator)))

  def orderShippingMethodDeleted(
      originator: Originator,
      cart: CartResponse,
      shippingMethod: ShippingMethod)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CartShippingMethodRemoved(cart, shippingMethod, buildOriginator(originator)))

  /* Cart Coupons */
  def orderCouponAttached(cart: Cart, couponCode: CouponCode)(implicit ec: EC,
                                                              ac: AC): DbResultT[Activity] =
    Activities.log(CartCouponAttached(cart, couponCode))

  def orderCouponDetached(cart: Cart)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CartCouponDetached(cart))

  /* Categories */
  def fullCategoryCreated(
      admin: Option[StoreAdmin],
      category: FullCategoryResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(FullCategoryCreated(admin.map(buildAdmin), category, context))

  def fullCategoryUpdated(
      admin: Option[StoreAdmin],
      category: FullCategoryResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(FullCategoryUpdated(admin.map(buildAdmin), category, context))

  /* Products */
  def fullProductCreated(
      admin: Option[StoreAdmin],
      product: ProductResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(FullProductCreated(admin.map(buildAdmin), product, context))

  def fullProductUpdated(
      admin: Option[StoreAdmin],
      product: ProductResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(FullProductUpdated(admin.map(buildAdmin), product, context))

  /* SKUs */
  def fullSkuCreated(
      admin: Option[StoreAdmin],
      product: SkuResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(FullSkuCreated(admin.map(buildAdmin), product, context))

  def fullSkuUpdated(
      admin: Option[StoreAdmin],
      product: SkuResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(FullSkuUpdated(admin.map(buildAdmin), product, context))

  /* Coupons */
  def couponCreated(couponResponse: CouponResponse.Root,
                    admin: Option[StoreAdmin])(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CouponCreated(couponResponse, admin.map(buildAdmin(_))))

  def couponUpdated(couponResponse: CouponResponse.Root,
                    admin: Option[StoreAdmin])(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(CouponUpdated(couponResponse, admin.map(buildAdmin(_))))

  def singleCouponCodeCreated(coupon: Coupon, admin: Option[StoreAdmin])(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(SingleCouponCodeGenerated(coupon, admin.map(buildAdmin(_))))

  def multipleCouponCodeCreated(coupon: Coupon, admin: Option[StoreAdmin])(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(MultipleCouponCodesGenerated(coupon, admin.map(buildAdmin(_))))

  /* Store Admin */
  def storeAdminCreated(entity: StoreAdmin, admin: Originator)(implicit ec: EC,
                                                               ac: AC): DbResultT[Activity] =
    Activities.log(StoreAdminCreated(entity, admin))

  def storeAdminUpdated(entity: StoreAdmin, admin: Originator)(implicit ec: EC,
                                                               ac: AC): DbResultT[Activity] =
    Activities.log(StoreAdminUpdated(entity, admin))

  def storeAdminDeleted(entity: StoreAdmin, admin: Originator)(implicit ec: EC,
                                                               ac: AC): DbResultT[Activity] =
    Activities.log(StoreAdminDeleted(entity, admin))

  def storeAdminStateChanged(entity: StoreAdmin,
                             oldState: StoreAdmin.State,
                             newState: StoreAdmin.State,
                             admin: Originator)(implicit ec: EC, ac: AC): DbResultT[Activity] =
    Activities.log(StoreAdminStateChanged(entity, oldState, newState, admin))

  /* Mail stuff */

  def sendMail(name: String, subject: String, email: String, html: String)(
      implicit ec: EC,
      ac: AC): DbResultT[Activity] =
    Activities.log(SendSimpleMail(name = name, subject = subject, email = email, html = html))

  /* Helpers */
  private def buildOriginator(originator: Originator): Option[AdminResponse] = originator match {
    case AdminOriginator(admin) ⇒ Some(buildAdmin(admin))
    case CustomerOriginator(_) ⇒
      None // We don't need customer, he's already in FullOrder.Root / Customer object
  }
}
