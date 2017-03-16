package services

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import models.Assignment._
import models.Note
import models.account.User
import models.activity.{Activities, Activity}
import models.admin.AdminData
import models.cord.{Cart, Order}
import models.coupon.{Coupon, CouponCode}
import models.location.Region
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCardCharge}
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.sharedsearch.SharedSearch
import models.shipping.ShippingMethod
import payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import responses.CategoryResponses.FullCategoryResponse
import responses.CouponResponses.CouponResponse
import responses.CreditCardsResponse.{buildSimple ⇒ buildCc}
import responses.CustomerResponse.{Root ⇒ CustomerResponse}
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses.ProductResponse
import responses.PromotionResponses.PromotionResponse
import responses.SkuResponses.SkuResponse
import responses.UserResponse.{Root ⇒ UserResponse, build ⇒ buildUser}
import responses.cord.{CartResponse, OrderResponse}
import responses.{AddressResponse, CaptureResponse, CreditCardsResponse, GiftCardResponse, StoreCreditResponse}
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
import services.activity.PromotionTailored._
import services.activity.SharedSearchTailored._
import services.activity.SkuTailored._
import services.activity.StoreAdminsTailored._
import services.activity.StoreCreditTailored._
import services.activity.UserTailored._
import utils.aliases._
import utils.db._

case class LogActivity(implicit ac: AC) {

  def withScope(scope: LTree): LogActivity = copy()(ac = ac.copy(scope = scope))

  /* Assignments */
  def assigned[T](admin: User,
                  entity: T,
                  assignees: Seq[UserResponse],
                  assignType: AssignmentType,
                  refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] = {
    Activities.log(Assigned[T](buildUser(admin), entity, assignees, assignType, refType))
  }

  def unassigned[T](admin: User,
                    entity: T,
                    assignee: User,
                    assignType: AssignmentType,
                    refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] = {
    Activities.log(
        Unassigned[T](buildUser(admin), entity, buildUser(assignee), assignType, refType))
  }

  def bulkAssigned(admin: User,
                   assignee: User,
                   entityIds: Seq[String],
                   assignType: AssignmentType,
                   refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] = {
    Activities.log(
        BulkAssigned(buildUser(admin), buildUser(assignee), entityIds, assignType, refType))
  }

  def bulkUnassigned(admin: User,
                     assignee: User,
                     entityIds: Seq[String],
                     assignType: AssignmentType,
                     refType: ReferenceType)(implicit ec: EC): DbResultT[Activity] = {
    Activities.log(
        BulkUnassigned(buildUser(admin), buildUser(assignee), entityIds, assignType, refType))
  }

  /* Notes */
  def noteCreated[T](admin: User, entity: T, note: Note)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(NoteCreated[T](buildUser(admin), entity, note))

  def noteUpdated[T](admin: User, entity: T, oldNote: Note, note: Note)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(NoteUpdated[T](buildUser(admin), entity, oldNote, note))

  def noteDeleted[T](admin: User, entity: T, note: Note)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(NoteDeleted[T](buildUser(admin), entity, note))

  /* Shared Search Associations */
  def associatedWithSearch(admin: User, search: SharedSearch, associates: Seq[User])(
      implicit ec: EC): DbResultT[Activity] = {
    Activities.log(AssociatedWithSearch(buildUser(admin), search, associates.map(buildUser)))
  }

  def unassociatedFromSearch(admin: User, search: SharedSearch, associate: User)(
      implicit ec: EC): DbResultT[Activity] = {
    Activities.log(UnassociatedFromSearch(buildUser(admin), search, buildUser(associate)))
  }

  /* Customer */
  def customerCreated(user: CustomerResponse, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    admin match {
      case Some(a) ⇒
        Activities.log(CustomerCreated(buildUser(a), user))
      case _ ⇒
        Activities.log(CustomerRegistered(user))
    }

  def customerUpdated(user: User, updated: User, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CustomerUpdated(buildUser(user), buildUser(updated), admin.map(buildUser)))

  def customerActivated(user: CustomerResponse, admin: User)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CustomerActivated(buildUser(admin), user))

  /* Users */
  def userCreated(user: UserResponse, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    admin match {
      case Some(a) ⇒
        Activities.log(UserCreated(buildUser(a), user))
      case _ ⇒
        Activities.log(UserRegistered(user))
    }

  def userActivated(user: UserResponse, admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserActivated(buildUser(admin), user))

  def userUpdated(user: User, updated: User, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserUpdated(buildUser(user), buildUser(updated), admin.map(buildUser)))

  def userDisabled(disabled: Boolean, user: User, admin: User)(
      implicit ec: EC): DbResultT[Activity] = {

    val adminResponse = buildUser(admin)
    val userResponse  = buildUser(user)

    if (disabled) {
      Activities.log(UserDisabled(adminResponse, userResponse))
    } else {
      Activities.log(UserEnabled(adminResponse, userResponse))
    }
  }

  def userBlacklisted(blacklisted: Boolean, user: User, admin: User)(
      implicit ec: EC): DbResultT[Activity] = {

    val adminResponse = buildUser(admin)
    val userResponse  = buildUser(user)

    if (blacklisted) {
      Activities.log(UserBlacklisted(adminResponse, userResponse))
    } else {
      Activities.log(UserRemovedFromBlacklist(adminResponse, userResponse))
    }
  }

  def userRemindPassword(user: User, code: String)(implicit ec: EC): DbResultT[Activity] = {
    val userResponse = buildUser(user)
    Activities.log(UserRemindPassword(user = userResponse, code = code))
  }

  def userPasswordReset(user: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserPasswordReset(user = buildUser(user)))

  def userPasswordChanged(user: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserPasswordChanged(user = buildUser(user)))

  /* User Addresses */
  def addressCreated(originator: User, user: User, address: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] = {
    Activities.log(UserAddressCreated(buildUser(user), address, buildOriginator(originator)))
  }

  def addressUpdated(originator: User,
                     user: User,
                     newAddress: AddressResponse,
                     oldAddress: AddressResponse)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        UserAddressUpdated(user = buildUser(user),
                           newInfo = newAddress,
                           oldInfo = oldAddress,
                           admin = buildOriginator(originator)))

  def addressDeleted(originator: User, user: User, address: AddressResponse)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(UserAddressDeleted(buildUser(user), address, buildOriginator(originator)))

  /* Users Credit Cards */
  def ccCreated(user: User, cc: CreditCard, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CreditCardAdded(buildUser(user), buildCc(cc), admin.map(buildUser)))

  def ccUpdated(user: User, newCc: CreditCard, oldCc: CreditCard, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        CreditCardUpdated(buildUser(user), buildCc(newCc), buildCc(oldCc), admin.map(buildUser)))

  def ccDeleted(user: User, cc: CreditCard, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CreditCardRemoved(buildUser(user), buildCc(cc), admin.map(buildUser)))

  /* Gift Cards */
  def gcCreated(admin: User, giftCard: GiftCard)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(GiftCardCreated(buildUser(admin), GiftCardResponse.build(giftCard)))

  def gcUpdated(admin: User, giftCard: GiftCard, payload: GiftCardUpdateStateByCsr)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        GiftCardStateChanged(buildUser(admin), GiftCardResponse.build(giftCard), payload))

  def gcConvertedToSc(admin: User, gc: GiftCard, sc: StoreCredit)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        GiftCardConvertedToStoreCredit(buildUser(admin),
                                       GiftCardResponse.build(gc),
                                       StoreCreditResponse.build(sc)))

  def gcFundsAuthorized(user: User, cart: Cart, gcCodes: Seq[String], amount: Int)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(GiftCardAuthorizedFunds(buildUser(user), cart, gcCodes, amount))

  def gcFundsCaptured(user: User, order: Order, gcCodes: Seq[String], amount: Int)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(GiftCardCapturedFunds(buildUser(user), order, gcCodes, amount))

  /* Store Credits */
  def scCreated(admin: User, user: User, sc: StoreCredit)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        StoreCreditCreated(buildUser(admin), buildUser(user), StoreCreditResponse.build(sc)))

  def scUpdated(admin: User, sc: StoreCredit, payload: StoreCreditUpdateStateByCsr)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        StoreCreditStateChanged(buildUser(admin), StoreCreditResponse.build(sc), payload))

  def scConvertedToGc(admin: User, gc: GiftCard, sc: StoreCredit)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        StoreCreditConvertedToGiftCard(buildUser(admin),
                                       GiftCardResponse.build(gc),
                                       StoreCreditResponse.build(sc)))

  def scFundsAuthorized(user: User, cart: Cart, scIds: Seq[Int], amount: Int)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreCreditAuthorizedFunds(buildUser(user), cart, scIds, amount))

  def scFundsCaptured(user: User, order: Order, scIds: Seq[Int], amount: Int)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreCreditCapturedFunds(buildUser(user), order, scIds, amount))

  /* Carts */
  def cartCreated(admin: Option[User], cart: CartResponse)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartCreated(admin.map(buildUser), cart))

  /* Orders */
  def orderStateChanged(admin: User, order: OrderResponse, oldState: Order.State)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderStateChanged(buildUser(admin), order, oldState))

  def orderBulkStateChanged(newState: Order.State,
                            cordRefNums: Seq[String],
                            admin: Option[User] = None)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderBulkStateChanged(admin.map(buildUser), cordRefNums, newState))

  def orderRemorsePeriodIncreased(
      admin: User,
      order: OrderResponse,
      oldPeriodEnd: Option[Instant])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderRemorsePeriodIncreased(buildUser(admin), order, oldPeriodEnd))

  /* Cart Line Items */
  def orderLineItemsAddedGc(admin: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartLineItemsAddedGiftCard(buildUser(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsUpdatedGc(admin: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        CartLineItemsUpdatedGiftCard(buildUser(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsDeletedGc(admin: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        CartLineItemsDeletedGiftCard(buildUser(admin), cart, GiftCardResponse.build(gc)))

  def orderLineItemsUpdated(cart: CartResponse,
                            oldQtys: Map[String, Int],
                            payload: Seq[UpdateLineItemsPayload],
                            admin: Option[User] = None)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        CartLineItemsUpdatedQuantities(cart,
                                       oldQtys,
                                       foldQuantityPayload(payload),
                                       admin.map(buildUser)))

  /* Order checkout & payments */

  def orderCheckoutCompleted(order: OrderResponse)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(OrderCheckoutCompleted(order))

  def orderCaptured(order: Order, cap: CaptureResponse)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        OrderCaptured(orderNum = order.referenceNumber,
                      accountId = order.accountId,
                      captured = cap.captured,
                      external = cap.external,
                      internal = cap.internal,
                      lineItems = cap.lineItems,
                      taxes = cap.taxes,
                      shipping = cap.shipping,
                      currency = cap.currency))

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

  def creditCardCharge(order: Order, charge: CreditCardCharge)(
      implicit ec: EC): DbResultT[Activity] =
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
  def orderPaymentMethodAddedCc(originator: User,
                                cart: CartResponse,
                                cc: CreditCard,
                                region: Region)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        CartPaymentMethodAddedCreditCard(cart,
                                         CreditCardsResponse.build(cc, region),
                                         buildOriginator(originator)))

  def orderPaymentMethodAddedGc(originator: User, cart: CartResponse, gc: GiftCard, amount: Int)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        CartPaymentMethodAddedGiftCard(cart,
                                       GiftCardResponse.build(gc),
                                       amount,
                                       buildOriginator(originator)))

  def orderPaymentMethodUpdatedGc(originator: User,
                                  cart: CartResponse,
                                  gc: GiftCard,
                                  oldAmount: Option[Int],
                                  amount: Int)(implicit ec: EC): DbResultT[Activity] = {
    val activity = CartPaymentMethodUpdatedGiftCard(cart,
                                                    GiftCardResponse.build(gc),
                                                    oldAmount,
                                                    amount,
                                                    buildOriginator(originator))

    Activities.log(activity)
  }

  def orderPaymentMethodAddedSc(originator: User, cart: CartResponse, amount: Int)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartPaymentMethodAddedStoreCredit(cart, amount, buildOriginator(originator)))

  def orderPaymentMethodDeleted(originator: User, cart: CartResponse, pmt: PaymentMethod.Type)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartPaymentMethodDeleted(cart, pmt, buildOriginator(originator)))

  def orderPaymentMethodDeletedGc(originator: User, cart: CartResponse, gc: GiftCard)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(
        CartPaymentMethodDeletedGiftCard(cart,
                                         GiftCardResponse.build(gc),
                                         buildOriginator(originator)))

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

  def orderShippingMethodDeleted(
      originator: User,
      cart: CartResponse,
      shippingMethod: ShippingMethod)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartShippingMethodRemoved(cart, shippingMethod, buildOriginator(originator)))

  /* Cart Coupons */
  def orderCouponAttached(cart: Cart, couponCode: CouponCode)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartCouponAttached(cart, couponCode))

  def orderCouponDetached(cart: Cart)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(CartCouponDetached(cart))

  /* Categories */
  def fullCategoryCreated(
      admin: Option[User],
      category: FullCategoryResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullCategoryCreated(admin.map(buildUser), category, context))

  def fullCategoryUpdated(
      admin: Option[User],
      category: FullCategoryResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullCategoryUpdated(admin.map(buildUser), category, context))

  /* Products */
  def fullProductCreated(
      admin: Option[User],
      product: ProductResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullProductCreated(admin.map(buildUser), product, context))

  def fullProductUpdated(
      admin: Option[User],
      product: ProductResponse.Root,
      context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullProductUpdated(admin.map(buildUser), product, context))

  /* SKUs */
  def fullSkuCreated(admin: Option[User],
                     product: SkuResponse.Root,
                     context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullSkuCreated(admin.map(buildUser), product, context))

  def fullSkuUpdated(admin: Option[User],
                     product: SkuResponse.Root,
                     context: ObjectContextResponse.Root)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(FullSkuUpdated(admin.map(buildUser), product, context))

  /* Promotions */
  def promotionCreated(promotionResponse: PromotionResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(PromotionCreated(promotionResponse, admin.map(buildUser(_))))

  def promotionUpdated(promotionResponse: PromotionResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(PromotionUpdated(promotionResponse, admin.map(buildUser(_))))

  /* Coupons */
  def couponCreated(couponResponse: CouponResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CouponCreated(couponResponse, admin.map(buildUser(_))))

  def couponUpdated(couponResponse: CouponResponse.Root, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(CouponUpdated(couponResponse, admin.map(buildUser(_))))

  def singleCouponCodeCreated(coupon: Coupon, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(SingleCouponCodeGenerated(coupon, admin.map(buildUser(_))))

  def multipleCouponCodeCreated(coupon: Coupon, admin: Option[User])(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(MultipleCouponCodesGenerated(coupon, admin.map(buildUser(_))))

  /* Store Admin */
  def storeAdminCreated(entity: User, admin: Option[User])(implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminCreated(entity, admin))

  def storeAdminUpdated(entity: User, admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminUpdated(entity, admin))

  def storeAdminDeleted(entity: User, admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminDeleted(entity, admin))

  def storeAdminStateChanged(entity: User,
                             oldState: AdminData.State,
                             newState: AdminData.State,
                             admin: User)(implicit ec: EC): DbResultT[Activity] =
    Activities.log(StoreAdminStateChanged(entity, oldState, newState, admin))

  /* Mail stuff */

  def sendMail(name: String, subject: String, email: String, html: String)(
      implicit ec: EC): DbResultT[Activity] =
    Activities.log(SendSimpleMail(name = name, subject = subject, email = email, html = html))

  /* Helpers */
  private def buildOriginator(originator: User): Option[UserResponse] =
    Some(buildUser(originator))
}
