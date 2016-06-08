package services

import java.time.Instant

import cats.implicits._
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.InventoryFailures.NotEnoughItems
import failures.PromotionFailures.PromotionNotFoundForContext
import failures.{Failures, GeneralFailure}
import models.coupon.{CouponCodes, Coupons, IlluminatedCoupon}
import models.customer.{Customer, Customers}
import models.inventory.summary.InventorySummaries
import models.inventory.{Sku, Skus}
import models.objects.{ObjectContext, ObjectContexts, ObjectForms, ObjectShadows}
import models.order.Order.RemorseHold
import models.order._
import models.order.lineitems.{OrderLineItemGiftCards, OrderLineItems}
import models.payment.creditcard.{CreditCardCharge, CreditCardCharges}
import models.payment.giftcard.{GiftCard, GiftCards}
import models.payment.storecredit.StoreCredits
import models.promotion.{IlluminatedPromotion, Promotions}
import responses.order.FullOrder
import services.coupon.CouponUsageService
import services.inventory.InventoryAdjustmentManager
import slick.driver.PostgresDriver.api._
import utils.apis.Apis
import utils.aliases._
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
      implicit ec: EC, db: DB, apis: Apis, ac: AC): Result[FullOrder.Root] =
    (for {
      result ← * <~ Orders
                .findActiveOrderByCustomer(customer)
                .one
                .findOrCreateExtended(Orders.create(Order.buildCart(customer.id, context.id)))
      (cart, _) = result
      order ← * <~ Checkout(cart, CartValidator(cart)).checkout
      _     ← * <~ LogActivity.orderCheckoutCompleted(order)
    } yield order).runTxn()
}

/*
  1) Run cart through validator
  2) Check inventory availability for every item (currently, that we have some items since our inventory is bunk)
  3) Re-validate that applied promos are active
  4) Authorize each payment method (stripe for cc, and gc and sc internally)
  5) Transition order to Remorse Hold**
  6) Create new cart for customer
 */
case class Checkout(cart: Order, cartValidator: CartValidation)(
    implicit ec: EC, db: DB, apis: Apis, ac: AC) {

  def checkout: DbResultT[FullOrder.Root] =
    for {
      _         ← * <~ cart.mustBeCart
      customer  ← * <~ Customers.mustFindById404(cart.customerId)
      _         ← * <~ checkInventory
      _         ← * <~ activePromos
      valid     ← * <~ cartValidator.validate(isCheckout = false, fatalWarnings = true)
      _         ← * <~ authPayments(customer)
      valid     ← * <~ cartValidator.validate(isCheckout = true, fatalWarnings = true)
      _         ← * <~ fraudScore
      _         ← * <~ remorseHold
      _         ← * <~ createNewCart
      _         ← * <~ updateCouponCountersForPromotion(customer)
      updated   ← * <~ Orders.refresh(cart).toXor
      _         ← * <~ InventoryAdjustmentManager.orderPlaced(cart)
      fullOrder ← * <~ FullOrder.fromOrder(updated).toXor
    } yield fullOrder

  private def checkInventory: DbResultT[Unit] =
    for {
      afs      ← * <~ InventorySummaries.getAvailableForSaleByOrderId(cart.id).result
      quantity ← * <~ OrderLineItems.countBySkuIdForOrder(cart).result
      _        ← * <~ checkAvailableEnough(afs.toMap, quantity.toMap)
    } yield {}

  def checkAvailableEnough(
      available: Map[Sku#Id, Int], required: Map[Sku#Id, Int]): DbResultT[Unit] = {
    def skuAsFailure(sku: Sku) =
      NotEnoughItems(sku.code, available.getOrElse(sku.id, 0), required.getOrElse(sku.id, 0))

    val newAfsValues  = available.remove(required)
    val invalidSkuIds = newAfsValues.collect { case (skuId, afsValue) if afsValue < 0 ⇒ skuId }

    for {
      invalidSkus ← * <~ Skus.filter(_.id inSet invalidSkuIds).result
      _           ← * <~ Failures(invalidSkus.map(skuAsFailure): _*).fold(DbResult.none)(DbResult.failures)
    } yield {}
  }

  private def activePromos: DbResultT[Unit] =
    for {
      maybePromo ← * <~ OrderPromotions.filterByOrderId(cart.id).one.toXor
      context    ← * <~ ObjectContexts.mustFindById400(cart.contextId)
      maybeCodeId = maybePromo.flatMap(_.couponCodeId)
      _ ← * <~ maybePromo.fold(DbResultT.unit)(promotionMustBeActive(_, context))
      _ ← * <~ maybeCodeId.fold(DbResultT.unit)(couponMustBeApplicable(_, context))
    } yield {}

  private def promotionMustBeActive(
      orderPromotion: OrderPromotion, context: ObjectContext): DbResultT[Unit] =
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
      maybePromo ← * <~ OrderPromotions.filterByOrderId(cart.id).one.toXor
      _ ← * <~ maybePromo.map { promo ⇒
           CouponUsageService.updateUsageCounts(promo.couponCodeId, cart.contextId, customer)
         }
    } yield {}

  private def authPayments(customer: Customer): DbResultT[Unit] =
    for {
      // Authorize GC payments
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByOrderId(cart.id).result
      giftCards ← * <~ authInternalPaymentMethod(
                     gcPayments, cart.grandTotal, GiftCards.authOrderPayment)
      gcAdjs = giftCards.map(_.getAmount.abs).sum

      // Authorize SC payments
      scPayments ← * <~ OrderPayments.findAllStoreCreditsByOrderId(cart.id).result
      storeCredits ← * <~ authInternalPaymentMethod(
                        scPayments, cart.grandTotal - gcAdjs, StoreCredits.authOrderPayment)
      scAdjs = storeCredits.map(_.getAmount.abs).sum

      // Log activities
      gcCodes = gcPayments.map { case (_, gc) ⇒ gc.code }.distinct
      scIds   = scPayments.map { case (_, sc) ⇒ sc.id }.distinct

      _ ← * <~ (if (gcAdjs > 0) LogActivity.gcFundsAuthorized(customer, cart, gcCodes, gcAdjs)
                else DbResult.unit)
      _ ← * <~ (if (scAdjs > 0) LogActivity.scFundsAuthorized(customer, cart, scIds, scAdjs)
                else DbResult.unit)

      // Authorize funds on credit card
      ccs ← * <~ authCreditCard(cart.grandTotal, gcAdjs + scAdjs)
    } yield {}

  private def authInternalPaymentMethod[M, Adj](
      payments: Seq[(OrderPayment, M)],
      maxPaymentAmount: Int,
      auth: (M, OrderPayment, Option[Int]) ⇒ DbResult[Adj]): DbResultT[Seq[Adj]] = {
    if (payments.isEmpty)
      DbResultT.pure(List.empty[Adj])
    else {
      val limitedAmounts = payments.map { case (payment, _) ⇒ payment.amount.getOrElse(0) }
        .foldLeft((maxPaymentAmount, List.empty[Int])) {
          case ((maxAmount, resultAmounts), amount) ⇒
            val thisAmount = maxAmount.min(amount)
            (maxAmount - thisAmount, thisAmount :: resultAmounts)
        }
        ._2
        .reverse

      DbResultT.sequence(
          payments
            .zip(limitedAmounts)
            .filter { case (_, amount) ⇒ amount > 0 }
            .map {
          case ((pmt, m), amount) ⇒ DbResultT(auth(m, pmt, amount.some))
        })
    }
  }

  private def authCreditCard(
      orderTotal: Int, internalPaymentTotal: Int): DbResult[Option[CreditCardCharge]] = {
    import scala.concurrent.duration._

    val authAmount = orderTotal - internalPaymentTotal

    if (authAmount > 0) {
      (for {
        pmt  ← OrderPayments.findAllCreditCardsForOrder(cart.id)
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
      remorseHold ← * <~ Orders.update(
                       cart, cart.copy(state = RemorseHold, placedAt = Instant.now.some))

      onHoldGcs ← * <~ (for {
                   items ← OrderLineItemGiftCards.findByOrderId(cart.id).result
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

  private def createNewCart: DbResult[Order] =
    Orders.create(Order.buildCart(cart.customerId, cart.contextId))
}
