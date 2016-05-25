package services.orders

import cats.data.Xor
import failures.OrderFailures._
import failures.CouponFailures._
import failures.PromotionFailures._
import failures.DiscountCompilerFailures._
import services.discount.compilers._
import models.discount.qualifiers._
import models.discount.offers._
import models.discount._
import models.discount.DiscountHelpers._
import models.objects._
import models.order._
import models.order.OrderPromotions.scope._
import models.order.lineitems._
import models.coupon._
import models.promotion._
import models.promotion.Promotions.scope._
import models.shipping
import models.traits.Originator
import responses.TheResponse
import responses.order.FullOrder
import responses.order.FullOrder._
import services.{CartValidator, Result}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object OrderPromotionUpdater {

  def attachCoupon(
      originator: Originator, refNum: Option[String] = None, context: ObjectContext, code: String)(
      implicit ec: EC, es: ES, db: DB): Result[TheResponse[FullOrder.Root]] =
    (for {
      // Fetch base data
      order ← * <~ getCartByOriginator(originator, refNum)
      _     ← * <~ order.mustBeCart
      orderPromotions ← * <~ OrderPromotions
                         .filterByOrderId(order.id)
                         .requiresCoupon
                         .one
                         .mustNotFindOr(OrderAlreadyHasCoupon)
      // Fetch coupon + validate
      couponCode ← * <~ CouponCodes.mustFindByCode(code)
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, couponCode.couponFormId)
                .one
                .mustFindOr(CouponWithCodeCannotBeFound(code))
      couponForm   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      couponShadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
      couponObject = IlluminatedCoupon.illuminate(context, coupon, couponForm, couponShadow)
      _ ← * <~ couponObject.mustBeActive
      _ ← * <~ couponObject.mustBeApplicable(couponCode, order.customerId)
      // Fetch promotion + validate
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(context.id, coupon.promotionId)
                   .requiresCoupon
                   .one
                   .mustFindOr(PromotionNotFoundForContext(coupon.promotionId, context.name))
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
      promoObject = IlluminatedPromotion.illuminate(context, promotion, promoForm, promoShadow)
      _ ← * <~ promoObject.mustBeActive
      // Fetch discount
      discountLinks ← * <~ ObjectLinks.filter(_.leftId === promoShadow.id).result
      discountShadowIds = discountLinks.map(_.rightId)
      discountShadows ← * <~ ObjectShadows.filter(_.id.inSet(discountShadowIds)).result
      discountFormIds = discountShadows.map(_.formId)
      discountForms ← * <~ ObjectForms.filter(_.id.inSet(discountFormIds)).result
      discounts = for (f ← discountForms; s ← discountShadows if s.formId == f.id) yield (f, s)
      // Safe AST compilation
      discount ← * <~ tryDiscount(discounts)
      (form, shadow) = discount
      qualifier   ← * <~ QualifierAstCompiler(qualifier(form, shadow)).compile()
      offer       ← * <~ OfferAstCompiler(offer(form, shadow)).compile()
      adjustments ← * <~ getAdjustments(promoShadow, order, qualifier, offer)
      // Create connected promotion and line item adjustments
      _ ← * <~ OrderPromotions.create(OrderPromotion.buildCoupon(order, promotion, couponCode))
      _ ← * <~ OrderLineItemAdjustments.createAll(adjustments)
      // Response
      order     ← * <~ OrderTotaler.saveTotals(order)
      validated ← * <~ CartValidator(order).validate()
      response  ← * <~ refreshAndFullOrder(order).toXor
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings))
      .runTxn()

  def detachCoupon(originator: Originator, refNum: Option[String] = None)(
      implicit ec: EC, es: ES, db: DB): Result[TheResponse[FullOrder.Root]] =
    (for {
      // Read
      order           ← * <~ getCartByOriginator(originator, refNum)
      _               ← * <~ order.mustBeCart
      orderPromotions ← * <~ OrderPromotions.filterByOrderId(order.id).requiresCoupon.result
      shadowIds = orderPromotions.map(_.promotionShadowId)
      promotions ← * <~ Promotions.filter(_.shadowId.inSet(shadowIds)).requiresCoupon.result
      deleteShadowIds = promotions.map(_.shadowId)
      // Write
      _         ← * <~ OrderPromotions.filterByOrderIdAndShadows(order.id, deleteShadowIds).delete
      _         ← * <~ OrderLineItemAdjustments.filterByOrderIdAndShadows(order.id, deleteShadowIds).delete
      _         ← * <~ OrderTotaler.saveTotals(order)
      validated ← * <~ CartValidator(order).validate()
      response  ← * <~ refreshAndFullOrder(order).toXor
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings))
      .runTxn()

  /**
    * Getting only first discount now
    */
  def tryDiscount(discounts: Seq[(ObjectForm, ObjectShadow)]) = discounts.headOption match {
    case Some(discount) ⇒ Xor.Right(discount)
    case _              ⇒ Xor.Left(EmptyDiscountFailure.single)
  }

  private def getAdjustments(
      promo: ObjectShadow, order: Order, qualifier: Qualifier, offer: Offer)(
      implicit ec: EC, es: ES, db: DB) =
    for {
      orderDetails ← * <~ fetchOrderDetails(order).toXor
      (lineItems, shippingMethod) = orderDetails
      input                       = DiscountInput(promo, order, lineItems, shippingMethod)
      _           ← * <~ qualifier.check(input)
      adjustments ← * <~ offer.adjust(input)
    } yield adjustments

  private def fetchOrderDetails(order: Order)(implicit ec: EC) =
    for {
      lineItemTup ← OrderLineItemSkus.findLineItemsByOrder(order).result
      lineItems = lineItemTup.map {
        case (sku, skuForm, skuShadow, productShadow, lineItem) ⇒
          OrderLineItemProductData(sku, skuForm, skuShadow, productShadow, lineItem)
      }
      shipMethod ← shipping.ShippingMethods.forOrder(order).one
    } yield (lineItems, shipMethod)
}
