package services

import scala.util.Random

import cats.data.Xor
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.GeneralFailure
import failures.PromotionFailures.PromotionNotFoundForContext
import models.account._
import models.cord._
import models.cord.lineitems.CartLineItems
import models.coupon._
import models.inventory.ProductVariants
import models.objects._
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.promotion._
import responses.cord.OrderResponse
import services.coupon.CouponUsageService
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
      cart ← * <~ Carts
              .findByAccountId(customer.accountId)
              .one
              .findOrCreate(
                  Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)))
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
    } yield order
}

class ExternalCalls {
  var authPaymentsSuccess: Boolean    = false
  var middleWarehouseSuccess: Boolean = false
}

case class Checkout(
    cart: Cart,
    cartValidator: CartValidation)(implicit ec: EC, db: DB, apis: Apis, ac: AC, ctx: OC, au: AU) {

  var externalCalls = new ExternalCalls()

  def checkout: Result[OrderResponse] = {
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
      _         ← * <~ LogActivity.orderCheckoutCompleted(fullOrder)
    } yield fullOrder

    actions.runTxn().map {
      case failures @ Xor.Left(_) ⇒
        if (externalCalls.middleWarehouseSuccess) cancelHoldInMiddleWarehouse
        failures

      case result @ Xor.Right(_) ⇒
        result
    }
  }

  private def holdInMiddleWarehouse(implicit ctx: OC): DbResultT[Unit] =
    for {
      prodVariantIds ← * <~ CartLineItems.byCordRef(cart.refNum).map(_.productVariantId).result
      skus           ← * <~ ProductVariants.filter(_.id.inSet(prodVariantIds)).map(_.code).result
      skuHold = skus.groupBy(identity).map {
        case (sku, occurrences) ⇒ SkuInventoryHold(sku, occurrences.size)
      }
      _ ← * <~ doOrMeh(skuHold.nonEmpty,
                       DbResultT.fromFuture(apis.middlewarehouse.hold(
                               OrderInventoryHold(cart.referenceNumber, skuHold.toSeq))))
      mutating = externalCalls.middleWarehouseSuccess = skuHold.nonEmpty
    } yield {}

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

      _ ← * <~ doOrMeh(scTotal > 0, LogActivity.scFundsAuthorized(customer, cart, scIds, scTotal))
      _ ← * <~ doOrMeh(gcTotal > 0,
                       LogActivity.gcFundsAuthorized(customer, cart, gcCodes, gcTotal))

      // Authorize funds on credit card
      ccs ← * <~ authCreditCard(cart.grandTotal, gcTotal + scTotal)
      mutatingResult = externalCalls.authPaymentsSuccess = true
    } yield {}

  private def authCreditCard(orderTotal: Int,
                             internalPaymentTotal: Int): DbResultT[Option[CreditCardCharge]] = {

    val authAmount = orderTotal - internalPaymentTotal

    if (authAmount > 0) {
      (for {
        pmt  ← OrderPayments.findAllCreditCardsForOrder(cart.refNum)
        card ← pmt.creditCard
      } yield (pmt, card)).one.dbresult.flatMap {
        case Some((pmt, card)) ⇒
          for {
            stripeCharge ← * <~ apis.stripe.authorizeAmount(card.gatewayCustomerId,
                                                            card.gatewayCardId,
                                                            authAmount,
                                                            cart.currency)
            ourCharge = CreditCardCharge.authFromStripe(card, pmt, stripeCharge, cart.currency)
            _       ← * <~ LogActivity.creditCardAuth(cart, ourCharge)
            created ← * <~ CreditCardCharges.create(ourCharge)
          } yield created.some

        case None ⇒
          DbResultT.failure(GeneralFailure("not enough payment"))
      }
    } else DbResultT.none
  }

  //TODO: Replace with the real deal once we figure out how to do it.
  private def fraudScore(order: Order): DbResultT[Order] =
    for {
      fakeFraudScore ← * <~ Random.nextInt(10)
      order          ← * <~ Orders.update(order, order.copy(fraudScore = fakeFraudScore))
    } yield order

}
