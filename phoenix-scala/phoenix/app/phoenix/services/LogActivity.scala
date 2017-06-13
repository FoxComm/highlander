package phoenix.services

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import core.db._
import objectframework.ObjectResponses.ObjectContextResponse
import phoenix.models.Assignment._
import phoenix.models.Note
import phoenix.models.account.User
import phoenix.models.activity.{Activities, Activity}
import phoenix.models.admin.AdminData
import phoenix.models.cord.{Cart, Order}
import phoenix.models.coupon.{Coupon, CouponCode}
import phoenix.models.customer.CustomerGroup
import phoenix.models.location.Region
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.applepay.{ApplePayCharge, ApplePayment}
import phoenix.models.payment.creditcard.{CreditCard, CreditCardCharge}
import phoenix.models.payment.giftcard.GiftCard
import phoenix.models.payment.storecredit.StoreCredit
import phoenix.models.returns.Return.State
import phoenix.models.returns._
import phoenix.models.sharedsearch.SharedSearch
import phoenix.models.shipping.ShippingMethod
import phoenix.payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.ReturnPayloads.{ReturnShippingCostLineItemPayload, ReturnSkuLineItemPayload}
import phoenix.payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import phoenix.responses.CategoryResponses.FullCategoryResponse
import phoenix.responses.CouponResponses.CouponResponse
import phoenix.responses.CreditCardsResponse.{buildSimple ⇒ buildCc}
import phoenix.responses.ProductResponses.ProductResponse
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses.SkuResponses.SkuResponse
import phoenix.responses._
import phoenix.responses.cord.{CartResponse, OrderResponse}
import phoenix.responses.users.{CustomerResponse, UserResponse}
import phoenix.services.carts.CartLineItemUpdater.foldQuantityPayload
import phoenix.services.activity.AssignmentsTailored._
import phoenix.services.activity.CartTailored._
import phoenix.services.activity.CatalogTailored._
import phoenix.services.activity.CategoryTailored._
import phoenix.services.activity.CouponsTailored._
import phoenix.services.activity.CustomerGroupsTailored._
import phoenix.services.activity.CustomerTailored._
import phoenix.services.activity.GiftCardTailored._
import phoenix.services.activity.MailTailored._
import phoenix.services.activity.NotesTailored._
import phoenix.services.activity.OrderTailored._
import phoenix.services.activity.ProductTailored._
import phoenix.services.activity.PromotionTailored._
import phoenix.services.activity.ReturnTailored._
import phoenix.services.activity.SharedSearchTailored._
import phoenix.services.activity.SkuTailored._
import phoenix.services.activity.StoreAdminsTailored._
import phoenix.services.activity.StoreCreditTailored._
import phoenix.services.activity.UserTailored._
import phoenix.utils.aliases._

case class LogActivity(implicit ac: AC) {

  def withScope(scope: LTree): LogActivity = copy()(ac = ac.copy(ctx = ac.ctx.copy(scope = scope)))

  /* Assignments */
  def assigned[T](admin: User,
                  entity: T,
                  assignees: Seq[UserResponse],
                  assignType: AssignmentType,
                  refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(Assigned[T](UserResponse.build(admin), entity, assignees, assignType, refType))

  def unassigned[T](admin: User,
                    entity: T,
                    assignee: User,
                    assignType: AssignmentType,
                    refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      Unassigned[T](UserResponse.build(admin), entity, UserResponse.build(assignee), assignType, refType))

  def bulkAssigned(admin: User,
                   assignee: User,
                   entityIds: Seq[String],
                   assignType: AssignmentType,
                   refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      BulkAssigned(UserResponse.build(admin), UserResponse.build(assignee), entityIds, assignType, refType))

  def bulkUnassigned(admin: User,
                     assignee: User,
                     entityIds: Seq[String],
                     assignType: AssignmentType,
                     refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      BulkUnassigned(UserResponse.build(admin), UserResponse.build(assignee), entityIds, assignType, refType))

  /* Notes */
  def noteCreated[T](admin: User, entity: T, note: Note)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(NoteCreated[T](UserResponse.build(admin), entity, note))

  def noteUpdated[T](admin: User, entity: T, oldNote: Note, note: Note)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(NoteUpdated[T](UserResponse.build(admin), entity, oldNote, note))

  def noteDeleted[T](admin: User, entity: T, note: Note)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(NoteDeleted[T](UserResponse.build(admin), entity, note))

  /* Shared Search Associations */
  def associatedWithSearch(admin: User, search: SharedSearch, associates: Seq[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      AssociatedWithSearch(UserResponse.build(admin), search, associates.map(UserResponse.build)))

  def unassociatedFromSearch(admin: User, search: SharedSearch, associate: User)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(UnassociatedFromSearch(UserResponse.build(admin), search, UserResponse.build(associate)))

  /* Customer */
  def customerCreated(user: CustomerResponse, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    admin match {
      case Some(a) ⇒
        Activities.log(CustomerCreated(UserResponse.build(a), user))
      case _ ⇒
        Activities.log(CustomerRegistered(user))
    }

  def customerUpdated(user: User, updated: User, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CustomerUpdated(UserResponse.build(user), UserResponse.build(updated), admin.map(UserResponse.build)))

  def customerActivated(user: CustomerResponse, admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CustomerActivated(UserResponse.build(admin), user))

  /* Users */
  // FIXME unused, do we need it? @aafa
  def userCreated(user: UserResponse, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    admin match {
      case Some(a) ⇒
        Activities.log(UserCreated(UserResponse.build(a), user))
      case _ ⇒
        Activities.log(UserRegistered(user))
    }

  // FIXME unused, do we need it? @aafa
  def userActivated(user: UserResponse, admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserActivated(UserResponse.build(admin), user))

  // FIXME unused, do we need it? @aafa
  def userUpdated(user: User, updated: User, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      UserUpdated(UserResponse.build(user), UserResponse.build(updated), admin.map(UserResponse.build)))

  def userDisabled(disabled: Boolean, user: User, admin: User)(implicit ec: EC): DbResultT[Activity] = {

    val adminResponse = UserResponse.build(admin)
    val userResponse  = UserResponse.build(user)

    if (disabled) {
      Activities.log(UserDisabled(adminResponse, userResponse))
    } else {
      Activities.log(UserEnabled(adminResponse, userResponse))
    }
  }

  def userBlacklisted(blacklisted: Boolean, user: User, admin: User)(implicit ec: EC): DbResultT[Activity] = {

    val adminResponse = UserResponse.build(admin)
    val userResponse  = UserResponse.build(user)

    if (blacklisted) {
      Activities.log(UserBlacklisted(adminResponse, userResponse))
    } else {
      Activities.log(UserRemovedFromBlacklist(adminResponse, userResponse))
    }
  }

  def userRemindPassword(user: User, code: String)(implicit ec: EC): DbResultT[Activity] = {
    val userResponse = UserResponse.build(user)
    Activities.log(UserRemindPassword(user = userResponse, code = code))
  }

  def userPasswordReset(user: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserPasswordReset(user = UserResponse.build(user)))

  def userPasswordChanged(user: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserPasswordChanged(user = UserResponse.build(user)))

  /* User Addresses */
  def addressCreated(originator: User, user: User, address: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserAddressCreated(UserResponse.build(user), address, buildOriginator(originator)))

  def addressUpdated(originator: User, user: User, newAddress: AddressResponse, oldAddress: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      UserAddressUpdated(user = UserResponse.build(user),
                         newInfo = newAddress,
                         oldInfo = oldAddress,
                         admin = buildOriginator(originator)))

  def addressDeleted(originator: User, user: User, address: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserAddressDeleted(UserResponse.build(user), address, buildOriginator(originator)))

  /* Users Credit Cards */
  def ccCreated(user: User, cc: CreditCard, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CreditCardAdded(UserResponse.build(user), buildCc(cc), admin.map(UserResponse.build)))

  def ccUpdated(user: User, newCc: CreditCard, oldCc: CreditCard, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CreditCardUpdated(UserResponse.build(user),
                        buildCc(newCc),
                        buildCc(oldCc),
                        admin.map(UserResponse.build)))

  def ccDeleted(user: User, cc: CreditCard, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CreditCardRemoved(UserResponse.build(user), buildCc(cc), admin.map(UserResponse.build)))

  /* Gift Cards */
  def gcCreated(admin: User, giftCard: GiftCard)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(GiftCardCreated(UserResponse.build(admin), GiftCardResponse.build(giftCard)))

  def gcUpdated(admin: User, giftCard: GiftCard, payload: GiftCardUpdateStateByCsr)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(GiftCardStateChanged(UserResponse.build(admin), GiftCardResponse.build(giftCard), payload))

  def gcConvertedToSc(admin: User, gc: GiftCard, sc: StoreCredit)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      GiftCardConvertedToStoreCredit(UserResponse.build(admin),
                                     GiftCardResponse.build(gc),
                                     StoreCreditResponse.build(sc)))

  def gcFundsAuthorized(user: User, cart: Cart, gcCodes: Seq[String], amount: Long)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(GiftCardAuthorizedFunds(UserResponse.build(user), cart, gcCodes, amount))

  def gcFundsCaptured(user: User, order: Order, gcCodes: Seq[String], amount: Long)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(GiftCardCapturedFunds(UserResponse.build(user), order, gcCodes, amount))

  /* Store Credits */
  def scCreated(admin: User, user: User, sc: StoreCredit)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      StoreCreditCreated(UserResponse.build(admin), UserResponse.build(user), StoreCreditResponse.build(sc)))

  def scUpdated(admin: User, sc: StoreCredit, payload: StoreCreditUpdateStateByCsr)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreCreditStateChanged(UserResponse.build(admin), StoreCreditResponse.build(sc), payload))

  def scConvertedToGc(admin: User, gc: GiftCard, sc: StoreCredit)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      StoreCreditConvertedToGiftCard(UserResponse.build(admin),
                                     GiftCardResponse.build(gc),
                                     StoreCreditResponse.build(sc)))

  def scFundsAuthorized(user: User, cart: Cart, scIds: Seq[Int], amount: Long)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreCreditAuthorizedFunds(UserResponse.build(user), cart, scIds, amount))

  def scFundsCaptured(user: User, order: Order, scIds: Seq[Int], amount: Long)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreCreditCapturedFunds(UserResponse.build(user), order, scIds, amount))

  /* Carts */
  def cartCreated(admin: Option[User], cart: CartResponse)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartCreated(admin.map(UserResponse.build), cart))

  /* Orders */
  def orderStateChanged(admin: User, order: OrderResponse, oldState: Order.State)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderStateChanged(UserResponse.build(admin), order, oldState))

  def orderBulkStateChanged(newState: Order.State, cordRefNums: Seq[String], admin: Option[User] = None)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderBulkStateChanged(admin.map(UserResponse.build), cordRefNums, newState))

  def orderRemorsePeriodIncreased(admin: User, order: OrderResponse, oldPeriodEnd: Option[Instant])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderRemorsePeriodIncreased(UserResponse.build(admin), order, oldPeriodEnd))

  /* Cart Line Items */
  def orderLineItemsAddedGc(admin: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartLineItemsAddedGiftCard(UserResponse.build(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsUpdatedGc(admin: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartLineItemsUpdatedGiftCard(UserResponse.build(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsDeletedGc(admin: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartLineItemsDeletedGiftCard(UserResponse.build(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsUpdated(cart: CartResponse,
                            oldQtys: Map[String, Int],
                            payload: Seq[UpdateLineItemsPayload],
                            admin: Option[User] = None)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CartLineItemsUpdatedQuantities(cart,
                                     oldQtys,
                                     foldQuantityPayload(payload),
                                     admin.map(UserResponse.build)))

  /* Order checkout & payments */

  def orderCheckoutCompleted(order: OrderResponse)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderCheckoutCompleted(order))

  def orderCaptured(order: Order, cap: CaptureResponse)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      OrderCaptured(
        orderNum = order.referenceNumber,
        accountId = order.accountId,
        captured = cap.captured,
        external = cap.external,
        internal = cap.internal,
        lineItems = cap.lineItems,
        taxes = cap.taxes,
        shipping = cap.shipping,
        currency = cap.currency
      ))

  def creditCardAuth(cart: Cart, charge: CreditCardCharge)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CreditCardAuthCompleted(
        accountId = cart.accountId,
        cordRef = cart.refNum,
        orderNum = cart.refNum,
        cardId = charge.creditCardId,
        amount = charge.amount,
        currency = charge.currency
      ))

  def applePayAuth(ap: ApplePayment, charge: ApplePayCharge)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      ApplePayAuthCompleted(
        accountId = ap.accountId,
        stripeTokenId = ap.stripeTokenId,
        amount = charge.amount,
        currency = charge.currency
      ))

  def creditCardCharge(order: Order, charge: CreditCardCharge)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CreditCardChargeCompleted(
        accountId = order.accountId,
        cordRef = order.refNum,
        orderNum = order.refNum,
        cardId = charge.creditCardId,
        amount = charge.amount,
        currency = charge.currency
      ))

  /* Cart Payment Methods */
  def orderPaymentMethodAddedCc(originator: User, cart: CartResponse, cc: CreditCard, region: Region)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CartPaymentMethodAddedCreditCard(cart,
                                       CreditCardsResponse.build(cc, region),
                                       buildOriginator(originator)))

  def orderPaymentMethodAddedGc(originator: User, cart: CartResponse, gc: GiftCard, amount: Long)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CartPaymentMethodAddedGiftCard(cart, GiftCardResponse.build(gc), amount, buildOriginator(originator)))

  def orderPaymentMethodUpdatedGc(originator: User,
                                  cart: CartResponse,
                                  gc: GiftCard,
                                  oldAmount: Option[Long],
                                  amount: Long)(implicit ec: EC): DbResultT[Activity] = {
    val activity = CartPaymentMethodUpdatedGiftCard(cart,
                                                    GiftCardResponse.build(gc),
                                                    oldAmount,
                                                    amount,
                                                    buildOriginator(originator))

    Activities.log(activity)
  }

  def orderPaymentMethodAddedSc(originator: User, cart: CartResponse, amount: Long)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartPaymentMethodAddedStoreCredit(cart, amount, buildOriginator(originator)))

  def orderPaymentMethodDeleted(originator: User, cart: CartResponse, pmt: PaymentMethod.Type)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartPaymentMethodDeleted(cart, pmt, buildOriginator(originator)))

  def orderPaymentMethodDeletedGc(originator: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
      CartPaymentMethodDeletedGiftCard(cart, GiftCardResponse.build(gc), buildOriginator(originator)))

  /* Cart Shipping Addresses */
  def orderShippingAddressAdded(originator: User, cart: CartResponse, address: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartShippingAddressAdded(cart, address, buildOriginator(originator)))

  def orderShippingAddressUpdated(originator: User, cart: CartResponse, address: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartShippingAddressUpdated(cart, address, buildOriginator(originator)))

  def orderShippingAddressDeleted(originator: User, cart: CartResponse, address: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartShippingAddressRemoved(cart, address, buildOriginator(originator)))

  /* Cart Shipping Methods */
  def orderShippingMethodUpdated(
      originator: User,
      cart: CartResponse,
      shippingMethod: Option[ShippingMethod])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartShippingMethodUpdated(cart, shippingMethod, buildOriginator(originator)))

  def orderShippingMethodDeleted(originator: User, cart: CartResponse, shippingMethod: ShippingMethod)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartShippingMethodRemoved(cart, shippingMethod, buildOriginator(originator)))

  /* Cart Coupons */
  def orderCouponAttached(cart: Cart, couponCode: CouponCode)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartCouponAttached(cart, couponCode))

  def orderCouponDetached(cart: Cart)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartCouponDetached(cart))

  /* Returns */
  def returnCreated(admin: User, rma: ReturnResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnCreated(UserResponse.build(admin), rma))

  def returnStateChanged(admin: User, rma: ReturnResponse.Root, oldState: State)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnStateChanged(UserResponse.build(admin), rma, oldState))

  def returnShippingCostItemAdded(
      rma: Return,
      reason: ReturnReason,
      payload: ReturnShippingCostLineItemPayload)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnShippingCostItemAdded(rma, reason, payload))

  def returnSkuLineItemAdded(rma: Return, reason: ReturnReason, payload: ReturnSkuLineItemPayload)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnSkuLineItemAdded(rma, reason, payload))

  def returnShippingCostItemDeleted(lineItem: ReturnLineItem)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnShippingCostItemDeleted(lineItem))

  def returnSkuLineItemDeleted(lineItem: ReturnLineItem)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnSkuLineItemDeleted(lineItem))

  def returnSkuLineItemsDropped(skus: List[ReturnLineItemSku])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnSkuLineItemsDropped(skus))

  def returnPaymentsAdded(rma: ReturnResponse.Root, payments: List[PaymentMethod.Type])(
      implicit ec: EC): DbResultT[Activity] = Activities.log(ReturnPaymentsAdded(rma, payments))

  def returnPaymentsDeleted(rma: ReturnResponse.Root, payments: List[PaymentMethod.Type])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnPaymentsDeleted(rma, payments))

  def issueCcRefund(rma: Return, payment: ReturnPayment)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnIssueCcRefund(rma, payment))

  def issueGcRefund(customer: User, rma: Return, gc: GiftCard)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnIssueGcRefund(customer, rma, gc))

  def issueScRefund(customer: User, rma: Return, sc: StoreCredit)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnIssueScRefund(customer, rma, sc))

  def cancelRefund(rma: Return)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ReturnCancelRefund(rma))

  /* Categories */
  def fullCategoryCreated(admin: Option[User],
                          category: FullCategoryResponse.Root,
                          context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullCategoryCreated(admin.map(UserResponse.build), category, context))

  def fullCategoryUpdated(admin: Option[User],
                          category: FullCategoryResponse.Root,
                          context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullCategoryUpdated(admin.map(UserResponse.build), category, context))

  /* Catalogs */
  def catalogCreated(
      admin: User,
      catalog: CatalogResponse.Root
  )(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CatalogCreated(UserResponse.build(admin), catalog))

  def catalogUpdated(
      admin: User,
      catalog: CatalogResponse.Root
  )(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CatalogUpdated(UserResponse.build(admin), catalog))

  def productsAddedToCatalog(
      admin: User,
      catalog: CatalogResponse.Root,
      productIds: Seq[Int]
  )(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ProductsAddedToCatalog(buildUser(admin), catalog, productIds))

  def productRemovedFromCatalog(
      admin: User,
      catalogId: Int,
      productId: Int
  )(implicit ec: EC): DbResultT[Activity] =
    Activities.log(ProductRemovedFromCatalog(buildUser(admin), catalogId, productId))

  /* Products */
  def fullProductCreated(admin: Option[User],
                         product: ProductResponse.Root,
                         context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullProductCreated(admin.map(UserResponse.build), product, context))

  def fullProductUpdated(admin: Option[User],
                         product: ProductResponse.Root,
                         context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullProductUpdated(admin.map(UserResponse.build), product, context))

  /* SKUs */
  def fullSkuCreated(admin: Option[User], product: SkuResponse.Root, context: ObjectContextResponse.Root)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullSkuCreated(admin.map(UserResponse.build), product, context))

  def fullSkuUpdated(admin: Option[User], product: SkuResponse.Root, context: ObjectContextResponse.Root)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullSkuUpdated(admin.map(UserResponse.build), product, context))

  /* Promotions */
  def promotionCreated(promotionResponse: PromotionResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(PromotionCreated(promotionResponse, admin.map(UserResponse.build(_))))

  def promotionUpdated(promotionResponse: PromotionResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(PromotionUpdated(promotionResponse, admin.map(UserResponse.build(_))))

  /* Coupons */
  def couponCreated(couponResponse: CouponResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CouponCreated(couponResponse, admin.map(UserResponse.build(_))))

  def couponUpdated(couponResponse: CouponResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CouponUpdated(couponResponse, admin.map(UserResponse.build(_))))

  def singleCouponCodeCreated(coupon: Coupon, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(SingleCouponCodeGenerated(coupon, admin.map(UserResponse.build(_))))

  def multipleCouponCodeCreated(coupon: Coupon, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(MultipleCouponCodesGenerated(coupon, admin.map(UserResponse.build(_))))

  /* Store Admin */
  def storeAdminCreated(entity: User, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminCreated(entity, admin))

  def storeAdminUpdated(entity: User, admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminUpdated(entity, admin))

  def storeAdminDeleted(entity: User, admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminDeleted(entity, admin))

  def storeAdminStateChanged(entity: User, oldState: AdminData.State, newState: AdminData.State, admin: User)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminStateChanged(entity, oldState, newState, admin))

  /* Customer Groups */
  def customerGroupCreated(customerGroup: CustomerGroup, admin: User)(implicit ec: EC,
                                                                      ac: AC): DbResultT[Activity] =
    Activities.log(CustomerGroupCreated(CustomerGroupActivity(customerGroup), admin))

  def customerGroupUpdated(customerGroup: CustomerGroup, admin: User)(implicit ec: EC,
                                                                      ac: AC): DbResultT[Activity] =
    Activities.log(CustomerGroupUpdated(CustomerGroupActivity(customerGroup), admin))

  def customerGroupArchived(customerGroup: CustomerGroup, admin: User)(implicit ec: EC,
                                                                       ac: AC): DbResultT[Activity] =
    Activities.log(CustomerGroupArchived(CustomerGroupActivity(customerGroup), admin))

  /* Mail stuff */

  def sendMail(name: String, subject: String, email: String, html: String)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(SendSimpleMail(name = name, subject = subject, email = email, html = html))

  /* Helpers */
  private def buildOriginator(originator: User): Option[UserResponse] =
    Some(UserResponse.build(originator))
}
