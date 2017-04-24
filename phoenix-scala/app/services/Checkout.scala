package services

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.AddressFailures.NoDefaultAddressForCustomer
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.CreditCardFailures.NoDefaultCreditCardForCustomer
import failures.GeneralFailure
import failures.PromotionFailures.PromotionNotFoundForContext
import failures.ShippingMethodFailures.NoDefaultShippingMethod
import models.account._
import models.cord._
import models.cord.lineitems.CartLineItems
import models.cord.lineitems.CartLineItems.scope._
import models.coupon._
import models.inventory.Skus
import models.location.Addresses
import models.objects._
import models.payment.applepay.{ApplePayCharge, ApplePayCharges, ApplePayment, ApplePayments}
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.promotion._
import models.shipping.DefaultShippingMethods
import org.json4s.JsonAST._
import payloads.CartPayloads.CheckoutCart
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads.CreateApplePayPayment
import responses.cord.OrderResponse
import scala.util.Random
import services.carts._
import services.coupon.CouponUsageService
import services.inventory.SkuManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis._
import utils.db._

object PaymentHelper {

  def paymentTransaction[Adjustment, Card](
      payments: Seq[(OrderPayment, Card)],
      maxPaymentAmount: Int,
      doTransaction: (Card, OrderPayment, Option[Int]) ⇒ DbResultT[Adjustment],
      getAdjustmentAmount: (Adjustment) ⇒ Int)(implicit ec: EC,
                                               db: DB,
                                               apis: Apis,
                                               ac: AC): DbResultT[Int] = {

    if (payments.isEmpty) {
      DbResultT.pure(0)
    } else {

      val amounts: Seq[Int] = payments.map { case (payment, _) ⇒ payment.getAmount() }
      val limitedAmounts = amounts
        .scan(maxPaymentAmount) {
          case (maxAmount, paymentAmount) ⇒ (maxAmount - paymentAmount).max(0)
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
                .findOrCreateExtended(Carts.create(
                        Cart(accountId = customer.accountId, scope = LTree(au.token.scope))))
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

  def applePayCheckout(customer: User, stripeToken: CreateApplePayPayment)(
      implicit ec: EC,
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
              .mustFindOr(GeneralFailure("Cart not found!"))
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
      LineItemUpdater.updateQuantitiesOnCustomersCart(customer, items).void

    for {
      cart ← * <~ CartQueries.findOrCreateCartByAccount(customer, ctx, admin)
      refNum     = cart.referenceNumber.some
      customerId = customer.accountId
      scope      = Scope.current

      stashedItems ← * <~ stashItems(cart.referenceNumber)
      _            ← * <~ LineItemUpdater.updateQuantitiesOnCustomersCart(customer, payload.items)

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
      _ ← * <~ doOrMeh(
             skusToHold.nonEmpty,
             DbResultT.fromResult(
                 apis.middlewarehouse.hold(OrderInventoryHold(cart.referenceNumber, skusToHold))))
      mutating = externalCalls.middleWarehouseSuccess = skusToHold.nonEmpty
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

  private def promotionMustBeActive(orderPromotion: OrderPromotion)(
      implicit ctx: OC): DbResultT[Unit] =
    for {
      promotion ← * <~ Promotions
                   .filterByContextAndShadowId(ctx.id, orderPromotion.promotionShadowId)
                   .mustFindOneOr(
                       PromotionNotFoundForContext(orderPromotion.promotionShadowId, ctx.name))
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
      scTotal ← * <~ PaymentHelper.paymentTransaction(
                   scPayments,
                   cart.grandTotal,
                   StoreCredits.authOrderPayment,
                   (a: StoreCreditAdjustment) ⇒ a.getAmount.abs
               )

      gcPayments ← * <~ OrderPayments.findAllGiftCardsByCordRef(cart.refNum).result
      gcTotal ← * <~ PaymentHelper.paymentTransaction(gcPayments,
                                                      cart.grandTotal - scTotal,
                                                      GiftCards.authOrderPayment,
                                                      (a: GiftCardAdjustment) ⇒ a.getAmount.abs)

      scIds   = scPayments.map { case (_, sc) ⇒ sc.id }.distinct
      gcCodes = gcPayments.map { case (_, gc) ⇒ gc.code }.distinct

      _ ← * <~ doOrMeh(scTotal > 0,
                       LogActivity().scFundsAuthorized(customer, cart, scIds, scTotal))
      _ ← * <~ doOrMeh(gcTotal > 0,
                       LogActivity().gcFundsAuthorized(customer, cart, gcCodes, gcTotal))

      grandTotal       = cart.grandTotal
      internalPayments = gcTotal + scTotal
      _ ← * <~ doOrMeh(
             grandTotal > internalPayments, // run external payments only if we have to pay more
             doExternalPayment(grandTotal, internalPayments))

      mutatingResult = externalCalls.authPaymentsSuccess = true
    } yield {}

  // do only one external payment. Check AP first, pay if it present otherwise try CC charge
  private def doExternalPayment(orderTotal: Int, internalPaymentTotal: Int): DbResultT[Unit] = {
    for {
      ap ← * <~ authApplePay(orderTotal, internalPaymentTotal)
      _  ← * <~ doOrMeh(ap.isEmpty, authCreditCard(orderTotal, internalPaymentTotal))
    } yield ()
  }

  private def authCreditCard(orderTotal: Int,
                             internalPaymentTotal: Int): DbResultT[Option[CreditCardCharge]] = {

    val authAmount = orderTotal - internalPaymentTotal

    (for {
      pmt  ← OrderPayments.findAllCreditCardsForOrder(cart.refNum)
      card ← pmt.creditCard
    } yield (pmt, card)).one.dbresult.flatMap {
      case Some((pmt, card)) ⇒
        for {
          stripeCharge ← * <~ apis.stripe.authorizeAmount(card.gatewayCardId,
                                                          authAmount,
                                                          cart.currency,
                                                          card.gatewayCustomerId.some)
          ourCharge = CreditCardCharge.authFromStripe(card, pmt, stripeCharge, cart.currency)
          _       ← * <~ LogActivity().creditCardAuth(cart, ourCharge)
          created ← * <~ CreditCardCharges.create(ourCharge)
        } yield created.some

      case None ⇒
        DbResultT.failure(GeneralFailure("not enough payment"))
    }
  }

  private def authApplePay(orderTotal: Int,
                           internalPaymentTotal: Int): DbResultT[Option[ApplePayCharge]] = {

    val authAmount = orderTotal - internalPaymentTotal

    (for {
      op ← OrderPayments.applePayByCordRef(cart.refNum)
      ap ← op.applePayment
    } yield (op, ap)).one.dbresult.flatMap {
      case Some((orderPayment, applePay)) ⇒
        for {
          stripeCharge ← * <~ apis.stripe
                          .authorizeApplePay(applePay.stripeTokenId, authAmount, cart.currency)
          ourCharge = ApplePayCharges
            .authFromStripe(applePay, orderPayment, stripeCharge, cart.currency)
          created ← * <~ ApplePayCharges.create(ourCharge)
          // todo logs here

        } yield created.some
      case _ ⇒ DbResultT.none // do nothing if apple pay is not there
    }
  }

  //TODO: Replace with the real deal once we figure out how to do it.
  private def fraudScore(order: Order): DbResultT[Order] =
    for {
      fakeFraudScore ← * <~ Random.nextInt(10)
      order          ← * <~ Orders.update(order, order.copy(fraudScore = fakeFraudScore))
    } yield order

}
