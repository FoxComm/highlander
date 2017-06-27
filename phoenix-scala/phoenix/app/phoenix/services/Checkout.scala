package phoenix.services

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.db._
import core.utils.Money._
import objectframework.ObjectUtils
import objectframework.models._
import org.json4s.JsonAST._
import phoenix.failures.AddressFailures.NoDefaultAddressForCustomer
import phoenix.failures.CartFailures.NoCartFound
import phoenix.failures.CouponFailures.CouponWithCodeCannotBeFound
import phoenix.failures.CreditCardFailures.NoDefaultCreditCardForCustomer
import phoenix.failures.OrderFailures.{ApplePayIsNotProvided, CreditCardIsNotProvided, NoExternalPaymentsIsProvided, OnlyOneExternalPaymentIsAllowed}
import phoenix.failures.PromotionFailures.PromotionNotFoundForContext
import phoenix.failures.ShippingMethodFailures.NoDefaultShippingMethod
import phoenix.models.account._
import phoenix.models.cord._
import phoenix.models.cord.lineitems.CartLineItems
import phoenix.models.cord.lineitems.CartLineItems.scope._
import phoenix.models.coupon._
import phoenix.models.inventory.Skus
import phoenix.models.location.Addresses
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.applepay.{ApplePayCharges, ApplePayments}
import phoenix.models.payment.creditcard._
import phoenix.models.payment.giftcard._
import phoenix.models.payment.storecredit._
import phoenix.models.promotion._
import phoenix.models.shipping.DefaultShippingMethods
import phoenix.payloads.CartPayloads.CheckoutCart
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.PaymentPayloads.CreateApplePayPayment
import phoenix.responses.cord.OrderResponse
import phoenix.services.carts._
import phoenix.services.coupon.CouponUsageService
import phoenix.services.inventory.SkuManager
import phoenix.utils.aliases._
import phoenix.utils.apis.{Apis, OrderInventoryHold, SkuInventoryHold}
import slick.jdbc.PostgresProfile.api._

import scala.util.Random

object PaymentHelper {

  def paymentTransaction[Adjustment, Card](
      payments: Seq[(OrderPayment, Card)],
      maxPaymentAmount: Long,
      doTransaction: (Card, OrderPayment, Option[Long]) ⇒ DbResultT[Adjustment],
      getAdjustmentAmount: (Adjustment) ⇒ Long)(implicit ec: EC,
                                                db: DB,
                                                apis: Apis,
                                                ac: AC): DbResultT[Long] =
    if (payments.isEmpty) {
      0L.pure[DbResultT]
    } else {

      val amounts: Seq[Long] = payments.map { case (payment, _) ⇒ payment.getAmount() }
      val limitedAmounts = amounts
        .scan(maxPaymentAmount) {
          case (maxAmount, paymentAmount) ⇒ (maxAmount - paymentAmount).zeroIfNegative
        }
        .zip(amounts)
        .map { case (max, amount) ⇒ Math.min(max, amount) }
        .ensuring(_.sum <= maxPaymentAmount)

      for {
        adjustments ← * <~ payments.zip(limitedAmounts).collect {
                       case ((payment, card), amount) if amount > 0 ⇒
                         doTransaction(card, payment, amount.some)
                     }
        total = adjustments.map(getAdjustmentAmount).sum.ensuring(_ <= maxPaymentAmount)
      } yield total
    }
}

object Checkout {

  def fromCart(refNum: String)(implicit ec: EC,
                               db: DB,
                               apis: Apis,
                               ac: AC,
                               ctx: OC,
                               au: AU): DbResultT[OrderResponse] =
    for {
      cart  ← * <~ Carts.mustFindByRefNum(refNum)
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
    } yield order

  def forCustomer(customer: User)(implicit ec: EC,
                                  db: DB,
                                  apis: Apis,
                                  ac: AC,
                                  ctx: OC,
                                  au: AU): DbResultT[OrderResponse] =
    for {
      result ← * <~ Carts
                .findByAccountId(customer.accountId)
                .one
                .findOrCreateExtended(
                  Carts.create(Cart(accountId = customer.accountId, scope = LTree(au.token.scope))))
      (cart, _) = result
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
    } yield order

  def forAdminOneClick(customerId: Int, payload: CheckoutCart)(implicit ec: EC,
                                                               db: DB,
                                                               apis: Apis,
                                                               ac: AC,
                                                               ctx: OC,
                                                               au: AU): DbResultT[OrderResponse] =
    for {
      customer ← * <~ Users.mustFindByAccountId(customerId)
      order    ← * <~ oneClickCheckout(customer, au.model.some, payload)
    } yield order

  def applePayCheckout(customer: User, stripeToken: CreateApplePayPayment)(implicit ec: EC,
                                                                           db: DB,
                                                                           apis: Apis,
                                                                           ac: AC,
                                                                           ctx: OC,
                                                                           au: AU): DbResultT[OrderResponse] =
    for {
      _ ← * <~ CartPaymentUpdater.addApplePayPayment(customer, stripeToken)
      cart ← * <~ Carts
              .findByAccountId(customer.accountId)
              .one
              .mustFindOr(NoCartFound(customer.accountId))
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
    } yield order

  def forCustomerOneClick(payload: CheckoutCart)(implicit ec: EC,
                                                 db: DB,
                                                 apis: Apis,
                                                 ac: AC,
                                                 ctx: OC,
                                                 au: AU): DbResultT[OrderResponse] =
    oneClickCheckout(au.model, None, payload)

  private def oneClickCheckout(customer: User, admin: Option[User], payload: CheckoutCart)(
      implicit ec: EC,
      db: DB,
      apis: Apis,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[OrderResponse] = {

    def stashItems(refNum: String): DbResultT[Seq[UpdateLineItemsPayload]] =
      CartLineItems
        .byCordRef(refNum)
        .join(Skus)
        .on(_.skuId === _.id)
        .result
        .dbresult
        .map(_.groupBy { case (cli, sku) ⇒ sku.code → cli.attributes }.map {
          case ((code, attrs), skus) ⇒
            UpdateLineItemsPayload(sku = code, quantity = skus.size, attributes = attrs)
        }.toStream)

    def unstashItems(items: Seq[UpdateLineItemsPayload]): DbResultT[Unit] =
      CartLineItemUpdater.updateQuantitiesOnCustomersCart(customer, items).void

    for {
      cart ← * <~ CartQueries.findOrCreateCartByAccount(customer, ctx, admin)
      refNum     = cart.referenceNumber.some
      customerId = customer.accountId
      scope      = Scope.current

      stashedItems ← * <~ stashItems(cart.referenceNumber)
      _            ← * <~ CartLineItemUpdater.updateQuantitiesOnCustomersCart(customer, payload.items)

      ccId ← * <~ CreditCards
              .findDefaultByAccountId(customerId)
              .map(_.id)
              .mustFindOneOr(NoDefaultCreditCardForCustomer())
      _ ← * <~ CartPaymentUpdater.addCreditCard(customer, ccId, refNum)

      addressId ← * <~ Addresses
                   .findShippingDefaultByAccountId(customerId)
                   .map(_.id)
                   .mustFindOneOr(NoDefaultAddressForCustomer())
      _ ← * <~ CartShippingAddressUpdater
           .createShippingAddressFromAddressId(customer, addressId, refNum)

      shippingMethod ← * <~ DefaultShippingMethods
                        .resolve(scope)
                        .mustFindOr(NoDefaultShippingMethod())
      _ ← * <~ CartShippingMethodUpdater.updateShippingMethod(customer, shippingMethod.id, refNum)

      cart  ← * <~ Carts.mustFindByRefNum(cart.referenceNumber)
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
      _     ← * <~ unstashItems(stashedItems)
    } yield order
  }
}

class ExternalCalls {
  // FIXME: have mercy… @michalrus
  @volatile var authPaymentsSuccess: Boolean    = false
  @volatile var middleWarehouseSuccess: Boolean = false
}

case class Checkout(
    cart: Cart,
    cartValidator: CartValidation)(implicit ec: EC, db: DB, apis: Apis, ac: AC, ctx: OC, au: AU) {

  val externalCalls = new ExternalCalls()

  def checkout: DbResultT[OrderResponse] = {
    val actions = for {
      customer  ← * <~ Users.mustFindByAccountId(cart.accountId)
      _         ← * <~ customer.mustHaveCredentials
      _         ← * <~ customer.mustNotBeBlacklisted
      _         ← * <~ activePromos
      _         ← * <~ cartValidator.validate(isCheckout = false, fatalWarnings = true)
      _         ← * <~ holdInMiddleWarehouse
      _         ← * <~ authPayments(customer)
      _         ← * <~ cartValidator.validate(isCheckout = true, fatalWarnings = true)
      order     ← * <~ Orders.createFromCart(cart, subScope = None)
      _         ← * <~ fraudScore(order)
      _         ← * <~ updateCouponCountersForPromotion(customer)
      fullOrder ← * <~ OrderResponse.fromOrder(order, grouped = true)
      _         ← * <~ LogActivity().orderCheckoutCompleted(fullOrder)
    } yield fullOrder

    actions.transformF(_.recoverWith {
      case failures if externalCalls.middleWarehouseSuccess ⇒
        DbResultT
          .fromResult(cancelHoldInMiddleWarehouse.mapEither {
            case Left(cancelationFailures) ⇒ Either.left(failures |+| cancelationFailures)
            case _                         ⇒ Either.left(failures)
          })
          .runEmpty
    })
  }

  private case class InventoryTrackedSku(isInventoryTracked: Boolean, code: String, qty: Int)

  private def holdInMiddleWarehouse(implicit ctx: OC): DbResultT[Unit] =
    for {
      liSkus               ← * <~ CartLineItems.byCordRef(cart.refNum).countSkus
      inventoryTrackedSkus ← * <~ filterInventoryTrackingSkus(liSkus)
      skusToHold           ← * <~ inventoryTrackedSkus.map(sku ⇒ SkuInventoryHold(sku.code, sku.qty))
      _ ← * <~ when(skusToHold.nonEmpty,
                       DbResultT.fromResult(
                         apis.middlewarehouse.hold(OrderInventoryHold(cart.referenceNumber, skusToHold))))
      mutating = externalCalls.middleWarehouseSuccess = skusToHold.nonEmpty // FIXME: I almost removed that, having read `==` here. Please, don’t… @michalrus
    } yield {}

  private def filterInventoryTrackingSkus(skus: Map[String, Int]) =
    for {
      skuInventoryData ← * <~ skus.map {
                          case (skuCode, qty) ⇒ isInventoryTracked(skuCode, qty)
                        }.toList
      // TODO: Add this back, but for gift cards we will track inventory (in the super short term).
      // inventoryTrackedSkus ← * <~ skuInventoryData.filter(_.isInventoryTracked)
    } yield skuInventoryData

  private def isInventoryTracked(skuCode: String, qty: Int) =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndCode(contextId = ctx.id, skuCode)
      shadow ← * <~ ObjectShadows.mustFindById400(sku.shadowId)
      form   ← * <~ ObjectForms.mustFindById400(shadow.formId)
      trackInventory = ObjectUtils.get("trackInventory", form, shadow) match {
        case JBool(trackInv) ⇒ trackInv
        case _               ⇒ true
      }
    } yield InventoryTrackedSku(trackInventory, skuCode, qty)

  private def cancelHoldInMiddleWarehouse: Result[Unit] =
    apis.middlewarehouse.cancelHold(cart.referenceNumber)

  private def activePromos: DbResultT[Unit] =
    for {
      maybePromo ← * <~ OrderPromotions.filterByCordRef(cart.refNum).one
      maybeCodeId = maybePromo.flatMap(_.couponCodeId)
      _ ← * <~ maybePromo.fold(DbResultT.unit)(promotionMustBeActive)
      _ ← * <~ maybeCodeId.fold(DbResultT.unit)(couponMustBeApplicable)
    } yield {}

  private def promotionMustBeActive(orderPromotion: OrderPromotion)(implicit ctx: OC): DbResultT[Unit] =
    for {
      promotion ← * <~ Promotions
                   .filterByContextAndShadowId(ctx.id, orderPromotion.promotionShadowId)
                   .mustFindOneOr(PromotionNotFoundForContext(orderPromotion.promotionShadowId, ctx.name))
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
      promoObject = IlluminatedPromotion.illuminate(ctx, promotion, promoForm, promoShadow)
      _ ← * <~ promoObject.mustBeActive
    } yield {}

  private def couponMustBeApplicable(codeId: Int)(implicit ctx: OC): DbResultT[Unit] =
    for {
      couponCode ← * <~ CouponCodes.findById(codeId).extract.one.safeGet
      coupon ← * <~ Coupons
                .filterByContextAndFormId(ctx.id, couponCode.couponFormId)
                .mustFindOneOr(CouponWithCodeCannotBeFound(couponCode.code))
      couponForm   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      couponShadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
      couponObject = IlluminatedCoupon.illuminate(ctx, coupon, couponForm, couponShadow)
      _ ← * <~ couponObject.mustBeActive
      _ ← * <~ couponObject.mustBeApplicable(couponCode, cart.accountId)
    } yield {}

  private def updateCouponCountersForPromotion(customer: User)(implicit ctx: OC): DbResultT[Unit] =
    for {
      maybePromo ← * <~ OrderPromotions.filterByCordRef(cart.refNum).one
      _ ← * <~ maybePromo.map { promo ⇒
           CouponUsageService.updateUsageCounts(promo.couponCodeId, customer)
         }
    } yield {}

  private def authPayments(customer: User): DbResultT[Unit] =
    for {

      scPayments ← * <~ OrderPayments.findAllStoreCreditsByCordRef(cart.refNum).result
      _          ← * <~ scPayments.map { case (_, sc) ⇒ DbResultT.fromEither(sc.mustBeActive) }
      scTotal ← * <~ PaymentHelper.paymentTransaction(
                 scPayments,
                 cart.grandTotal,
                 StoreCredits.authOrderPayment,
                 (a: StoreCreditAdjustment) ⇒ a.getAmount.abs
               )

      gcPayments ← * <~ OrderPayments.findAllGiftCardsByCordRef(cart.refNum).result
      _          ← * <~ gcPayments.map { case (_, gc) ⇒ DbResultT.fromEither(gc.mustBeActive) }
      gcTotal ← * <~ PaymentHelper.paymentTransaction(gcPayments,
                                                      cart.grandTotal - scTotal,
                                                      GiftCards.authOrderPayment,
                                                      (a: GiftCardAdjustment) ⇒ a.getAmount.abs)

      scIds   = scPayments.map { case (_, sc) ⇒ sc.id }.distinct
      gcCodes = gcPayments.map { case (_, gc) ⇒ gc.code }.distinct

      _ ← * <~ when(scTotal > 0, LogActivity().scFundsAuthorized(customer, cart, scIds, scTotal).void)
      _ ← * <~ when(gcTotal > 0, LogActivity().gcFundsAuthorized(customer, cart, gcCodes, gcTotal).void)

      grandTotal       = cart.grandTotal
      internalPayments = gcTotal + scTotal
      _ ← * <~ when(grandTotal > internalPayments, // run external payments only if we have to pay more
                       doExternalPayment(grandTotal - internalPayments).void)

      mutatingResult = externalCalls.authPaymentsSuccess = true // fixme is this flag used anywhere? @aafa
    } yield {}

  private def doExternalPayment(authAmount: Long): DbResultT[Unit] = {
    require(authAmount > 0)

    for {
      orderPayments ← * <~ OrderPayments.findAllExternalPayments(cart.refNum).result

      _ ← * <~ failIf(orderPayments.isEmpty, NoExternalPaymentsIsProvided)
      _ ← * <~ failIf(orderPayments.groupBy(_.paymentMethodType).size > 1, OnlyOneExternalPaymentIsAllowed)

      // authorize first payment we've got
      _ ← * <~ authOneExternalPayment(authAmount, orderPayments.head)
    } yield ()
  }

  private def authOneExternalPayment(authAmount: Long, orderPayment: OrderPayment): DbResultT[Unit] =
    orderPayment.paymentMethodType match {
      case PaymentMethod.ApplePay   ⇒ authApplePay(authAmount, orderPayment)
      case PaymentMethod.CreditCard ⇒ authCreditCard(authAmount, orderPayment)
      case _                        ⇒ DbResultT.unit
    }

  private def authCreditCard(authAmount: Long, orderPayment: OrderPayment): DbResultT[Unit] =
    for {
      card ← * <~ CreditCards
              .filter(_.id === orderPayment.paymentMethodId)
              .mustFindOneOr(CreditCardIsNotProvided)

      stripeCharge ← * <~ apis.stripe.authorizeAmount(card.gatewayCardId,
                                                      authAmount,
                                                      cart.currency,
                                                      card.gatewayCustomerId.some)

      ourCharge = CreditCardCharge.authFromStripe(card, orderPayment, stripeCharge, cart.currency)
      _ ← * <~ CreditCardCharges.create(ourCharge)
      _ ← * <~ LogActivity().creditCardAuth(cart, ourCharge)
    } yield ()

  private def authApplePay(authAmount: Long, orderPayment: OrderPayment): DbResultT[Unit] =
    for {
      applePay ← * <~ ApplePayments
                  .filter(_.id === orderPayment.paymentMethodId)
                  .mustFindOneOr(ApplePayIsNotProvided)

      stripeCharge ← * <~ apis.stripe
                      .authorizeAmount(applePay.stripeTokenId, authAmount, cart.currency)
      ourCharge = ApplePayCharges
        .authFromStripe(applePay, orderPayment, stripeCharge, cart.currency)
      _ ← * <~ ApplePayCharges.create(ourCharge)
      _ ← * <~ LogActivity().applePayAuth(applePay, ourCharge)
    } yield ()

  //TODO: Replace with the real deal once we figure out how to do it.
  private def fraudScore(order: Order): DbResultT[Order] =
    for {
      fakeFraudScore ← * <~ Random.nextInt(10)
      order          ← * <~ Orders.update(order, order.copy(fraudScore = fakeFraudScore))
    } yield order

}
