package services

import java.time.Instant

import cats.implicits._
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.GeneralFailure
import failures.PromotionFailures.PromotionNotFoundForContext
import models.coupon.{CouponCodes, Coupons, IlluminatedCoupon}
import models.customer.{Customer, Customers}
import models.objects.{ObjectContext, ObjectContexts, ObjectForms, ObjectShadows}
import models.order.Order.RemorseHold
import models.order._
import models.order.lineitems.OrderLineItemGiftCards
import models.payment.creditcard.{CreditCardCharge, CreditCardCharges}
import models.payment.giftcard.{GiftCard, GiftCardAdjustment, GiftCards}
import models.payment.storecredit.{StoreCreditAdjustment, StoreCredits}
import models.promotion.{IlluminatedPromotion, Promotions}
import responses.order.FullOrder
import services.coupon.CouponUsageService
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.Apis
import utils.db.DbResultT._
import utils.db._

object Checkout {

  def fromCart(
      refNum: String)(implicit ec: EC, db: DB, apis: Apis, ac: AC): Result[FullOrder.Root] =
    (for {
      cart  ← * <~ Orders.mustFindByRefNum(refNum)
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
      _     ← * <~ LogActivity.orderCheckoutCompleted(order)
    } yield order).runTxn()

  def fromCustomerCart(customer: Customer, context: ObjectContext)(
      implicit ec: EC,
      db: DB,
      apis: Apis,
      ac: AC): Result[FullOrder.Root] =
    (for {
      result ← * <~ Orders
                .findActiveOrderByCustomer(customer)
                .one
                // TODO @anna: #longlivedbresultt
                .findOrCreateExtended(
                    Orders.create(Order.buildCart(customer.id, context.id)).value)
      (cart, _) = result
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
      _     ← * <~ LogActivity.orderCheckoutCompleted(order)
    } yield order).runTxn()
}

case class Checkout(cart: Order,
                    cartValidator: CartValidation)(implicit ec: EC, db: DB, apis: Apis, ac: AC) {

  def checkout: DbResultT[FullOrder.Root] =
    for {
      _        ← * <~ cart.mustBeCart
      customer ← * <~ Customers.mustFindById404(cart.customerId)
      // TODO make request to #middlewarehouse
      _         ← * <~ activePromos
      valid     ← * <~ cartValidator.validate(isCheckout = false, fatalWarnings = true)
      _         ← * <~ authPayments(customer)
      valid     ← * <~ cartValidator.validate(isCheckout = true, fatalWarnings = true)
      _         ← * <~ fraudScore
      _         ← * <~ remorseHold
      _         ← * <~ createNewCart
      _         ← * <~ updateCouponCountersForPromotion(customer)
      updated   ← * <~ Orders.refresh(cart).toXor
      fullOrder ← * <~ FullOrder.fromOrder(updated).toXor
    } yield fullOrder

  private def activePromos: DbResultT[Unit] =
    for {
      maybePromo ← * <~ OrderPromotions.filterByOrderRef(cart.refNum).one.toXor
      context    ← * <~ ObjectContexts.mustFindById400(cart.contextId)
      maybeCodeId = maybePromo.flatMap(_.couponCodeId)
      _ ← * <~ maybePromo.fold(DbResultT.unit)(promotionMustBeActive(_, context))
      _ ← * <~ maybeCodeId.fold(DbResultT.unit)(couponMustBeApplicable(_, context))
    } yield {}

  private def promotionMustBeActive(orderPromotion: OrderPromotion,
                                    context: ObjectContext): DbResultT[Unit] =
    for {
      promotion ← * <~ Promotions
                   .filterByContextAndShadowId(context.id, orderPromotion.promotionShadowId)
                   .mustFindOneOr(
                       PromotionNotFoundForContext(orderPromotion.promotionShadowId, context.name))
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
      promoObject = IlluminatedPromotion.illuminate(context, promotion, promoForm, promoShadow)
      _ ← * <~ promoObject.mustBeActive
    } yield {}

  private def couponMustBeApplicable(codeId: Int, context: ObjectContext): DbResultT[Unit] =
    for {
      couponCode ← * <~ CouponCodes.findById(codeId).extract.one.safeGet.toXor
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, couponCode.couponFormId)
                .mustFindOneOr(CouponWithCodeCannotBeFound(couponCode.code))
      couponForm   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      couponShadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
      couponObject = IlluminatedCoupon.illuminate(context, coupon, couponForm, couponShadow)
      _ ← * <~ couponObject.mustBeActive
      _ ← * <~ couponObject.mustBeApplicable(couponCode, cart.customerId)
    } yield {}

  private def updateCouponCountersForPromotion(customer: Customer): DbResultT[Unit] =
    for {
      maybePromo ← * <~ OrderPromotions.filterByOrderRef(cart.refNum).one.toXor
      _ ← * <~ maybePromo.map { promo ⇒
           CouponUsageService.updateUsageCounts(promo.couponCodeId, cart.contextId, customer)
         }
    } yield {}

  private def authPayments(customer: Customer): DbResultT[Unit] =
    for {
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByOrderRef(cart.refNum).result
      gcTotal ← * <~ authInternalPaymentMethod(gcPayments,
                                               cart.grandTotal,
                                               GiftCards.authOrderPayment,
                                               (a: GiftCardAdjustment) ⇒ a.getAmount.abs)

      scPayments ← * <~ OrderPayments.findAllStoreCreditsByOrderRef(cart.refNum).result
      scTotal ← * <~ authInternalPaymentMethod(
                   scPayments,
                   cart.grandTotal - gcTotal,
                   StoreCredits.authOrderPayment,
                   (a: StoreCreditAdjustment) ⇒ a.getAmount.abs
               )

      gcCodes = gcPayments.map { case (_, gc) ⇒ gc.code }.distinct
      scIds   = scPayments.map { case (_, sc) ⇒ sc.id }.distinct

      _ ← * <~ (if (gcTotal > 0) LogActivity.gcFundsAuthorized(customer, cart, gcCodes, gcTotal)
                else DbResult.unit)
      _ ← * <~ (if (scTotal > 0) LogActivity.scFundsAuthorized(customer, cart, scIds, scTotal)
                else DbResult.unit)

      // Authorize funds on credit card
      ccs ← * <~ authCreditCard(cart.grandTotal, gcTotal + scTotal)
    } yield {}

  private def authInternalPaymentMethod[Adjustment, Card](
      orderPayments: Seq[(OrderPayment, Card)],
      maxPaymentAmount: Int,
      authOrderPayment: (Card, OrderPayment, Option[Int]) ⇒ DbResultT[Adjustment],
      getAdjustmentAmount: (Adjustment) ⇒ Int): DbResultT[Int] = {

    if (orderPayments.isEmpty) {
      DbResultT.pure(0)
    } else {

      val amounts: Seq[Int] = orderPayments.map { case (payment, _) ⇒ payment.getAmount() }
      val limitedAmounts = amounts
        .scan(maxPaymentAmount) {
          case (maxAmount, paymentAmount) ⇒ (maxAmount - paymentAmount).max(0)
        }
        .zip(amounts)
        .map { case (max, amount) ⇒ Math.min(max, amount) }
        .ensuring(_.sum <= maxPaymentAmount)

      for {
        adjustments ← * <~ orderPayments.zip(limitedAmounts).collect {
                       case ((payment, card), amount) if amount > 0 ⇒
                         authOrderPayment(card, payment, amount.some)
                     }
        total = adjustments.map(getAdjustmentAmount).sum.ensuring(_ <= maxPaymentAmount)
      } yield total
    }
  }

  private def authCreditCard(orderTotal: Int,
                             internalPaymentTotal: Int): DbResult[Option[CreditCardCharge]] = {
    import scala.concurrent.duration._

    val authAmount = orderTotal - internalPaymentTotal

    if (authAmount > 0) {
      (for {
        pmt  ← OrderPayments.findAllCreditCardsForOrder(cart.refNum)
        card ← pmt.creditCard
      } yield (pmt, card)).one.flatMap {
        case Some((pmt, card)) ⇒
          val f = Stripe().authorizeAmount(card.gatewayCustomerId, authAmount, cart.currency)

          (for {
            // TODO: remove the blocking Await which causes us to change types (I knew it was coming anyways!)
            stripeCharge ← * <~ scala.concurrent.Await.result(f, 5.seconds)
            ourCharge = CreditCardCharge.authFromStripe(card, pmt, stripeCharge, cart.currency)
            _       ← * <~ LogActivity.creditCardCharge(cart, ourCharge)
            created ← * <~ CreditCardCharges.create(ourCharge)
          } yield created.some).value

        case None ⇒
          DbResult.failure(GeneralFailure("not enough payment"))
      }
    } else {
      DbResult.none
    }
  }

  private def remorseHold: DbResult[Order] =
    (for {
      remorseHold ← * <~ Orders.update(cart,
                                       cart.copy(state = RemorseHold, placedAt = Instant.now.some))
      onHoldGcs ← * <~ (for {
                   items ← OrderLineItemGiftCards.findByOrderRef(cart.refNum).result
                   holds ← GiftCards
                            .filter(_.id.inSet(items.map(_.giftCardId)))
                            .map(_.state)
                            .update(GiftCard.OnHold)
                 } yield holds).toXor
    } yield remorseHold).value

  private def fraudScore: DbResult[Order] =
    (for {
      fraudScore ← * <~ scala.util.Random.nextInt(10)
      order      ← * <~ Orders.update(cart, cart.copy(fraudScore = fraudScore))
    } yield order).value

  private def createNewCart: DbResultT[Order] =
    Orders.create(Order.buildCart(cart.customerId, cart.contextId))
}
