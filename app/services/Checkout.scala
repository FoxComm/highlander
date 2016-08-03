package services

import scala.util.Random

import cats.implicits._
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.GeneralFailure
import failures.PromotionFailures.PromotionNotFoundForContext
import models.cord._
import models.cord.lineitems.{OrderLineItemGiftCards, OrderLineItemSkus}
import models.coupon._
import models.customer.{Customer, Customers}
import models.objects._
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.promotion._
import responses.cord.OrderResponse
import services.coupon.CouponUsageService
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.{Apis, OrderReservation, SkuReservation}
import utils.db._

object Checkout {

  def fromCart(refNum: String)(implicit ec: EC,
                               db: DB,
                               apis: Apis,
                               ac: AC,
                               ctx: OC): DbResultT[OrderResponse] =
    for {
      cart  ← * <~ Carts.mustFindByRefNum(refNum)
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
    } yield order

  def fromCustomerCart(customer: Customer)(implicit ec: EC,
                                           db: DB,
                                           apis: Apis,
                                           ac: AC,
                                           ctx: OC): DbResultT[OrderResponse] =
    for {
      result ← * <~ Carts
                .findByCustomer(customer)
                .one
                .findOrCreateExtended(Carts.create(Cart(customerId = customer.id)))
      (cart, _) = result
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
    } yield order
}

case class Checkout(
    cart: Cart,
    cartValidator: CartValidation)(implicit ec: EC, db: DB, apis: Apis, ac: AC, ctx: OC) {

  def checkout: DbResultT[OrderResponse] =
    for {
      customer  ← * <~ Customers.mustFindById404(cart.customerId)
      _         ← * <~ customer.mustHaveCredentials
      _         ← * <~ activePromos
      _         ← * <~ cartValidator.validate(isCheckout = false, fatalWarnings = true)
      _         ← * <~ reserveInMiddleWarehouse
      _         ← * <~ authPayments(customer)
      _         ← * <~ cartValidator.validate(isCheckout = true, fatalWarnings = true)
      order     ← * <~ Orders.create(cart.toOrder())
      _         ← * <~ fraudScore(order)
      _         ← * <~ remorseHold(order)
      _         ← * <~ updateCouponCountersForPromotion(customer)
      fullOrder ← * <~ OrderResponse.fromOrder(order)
      _         ← * <~ LogActivity.orderCheckoutCompleted(fullOrder)
    } yield fullOrder

  private def reserveInMiddleWarehouse: DbResultT[Unit] =
    for {
      liSkus ← * <~ OrderLineItemSkus.countSkusByCordRef(cart.refNum)
      skuReservations = liSkus.map { case (skuCode, qty) ⇒ SkuReservation(skuCode, qty) }.toSeq
      _ ← * <~ apis.middlwarehouse.reserve(OrderReservation(cart.referenceNumber, skuReservations))
    } yield {}

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
      _ ← * <~ couponObject.mustBeApplicable(couponCode, cart.customerId)
    } yield {}

  private def updateCouponCountersForPromotion(customer: Customer)(
      implicit ctx: OC): DbResultT[Unit] =
    for {
      maybePromo ← * <~ OrderPromotions.filterByCordRef(cart.refNum).one
      _ ← * <~ maybePromo.map { promo ⇒
           CouponUsageService.updateUsageCounts(promo.couponCodeId, customer)
         }
    } yield {}

  private def authPayments(customer: Customer): DbResultT[Unit] =
    for {
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByCordRef(cart.refNum).result
      gcTotal ← * <~ authInternalPaymentMethod(gcPayments,
                                               cart.grandTotal,
                                               GiftCards.authOrderPayment,
                                               (a: GiftCardAdjustment) ⇒ a.getAmount.abs)

      scPayments ← * <~ OrderPayments.findAllStoreCreditsByCordRef(cart.refNum).result
      scTotal ← * <~ authInternalPaymentMethod(
                   scPayments,
                   cart.grandTotal - gcTotal,
                   StoreCredits.authOrderPayment,
                   (a: StoreCreditAdjustment) ⇒ a.getAmount.abs
               )

      gcCodes = gcPayments.map { case (_, gc) ⇒ gc.code }.distinct
      scIds   = scPayments.map { case (_, sc) ⇒ sc.id }.distinct

      _ ← * <~ (if (gcTotal > 0) LogActivity.gcFundsAuthorized(customer, cart, gcCodes, gcTotal)
                else DbResultT.unit)
      _ ← * <~ (if (scTotal > 0) LogActivity.scFundsAuthorized(customer, cart, scIds, scTotal)
                else DbResultT.unit)

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
                             internalPaymentTotal: Int): DbResultT[Option[CreditCardCharge]] = {
    import scala.concurrent.duration._

    val authAmount = orderTotal - internalPaymentTotal

    if (authAmount > 0) {
      (for {
        pmt  ← OrderPayments.findAllCreditCardsForOrder(cart.refNum)
        card ← pmt.creditCard
      } yield (pmt, card)).one.toXor.flatMap {
        case Some((pmt, card)) ⇒
          val f = Stripe().authorizeAmount(card.gatewayCustomerId, authAmount, cart.currency)

          for {
            // TODO: remove the blocking Await which causes us to change types (I knew it was coming anyways!)
            stripeCharge ← * <~ scala.concurrent.Await.result(f, 5.seconds)
            ourCharge = CreditCardCharge.authFromStripe(card, pmt, stripeCharge, cart.currency)
            _       ← * <~ LogActivity.creditCardCharge(cart, ourCharge)
            created ← * <~ CreditCardCharges.create(ourCharge)
          } yield created.some

        case None ⇒
          DbResultT.failure(GeneralFailure("not enough payment"))
      }
    } else DbResultT.none
  }

  private def remorseHold(order: Order): DBIO[Unit] =
    for {
      items ← OrderLineItemGiftCards.findByOrderRef(cart.refNum).result
      holds ← GiftCards
               .filter(_.id.inSet(items.map(_.giftCardId)))
               .map(_.state)
               .update(GiftCard.OnHold)
    } yield {}

  private def fraudScore(order: Order): DbResultT[Order] =
    for {
      fraudScore ← * <~ Random.nextInt(10)
      order      ← * <~ Orders.update(order, order.copy(fraudScore = fraudScore))
    } yield order

}
