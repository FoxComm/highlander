package services.carts

import cats.data.Xor
import failures.CouponFailures._
import failures.DiscountCompilerFailures._
import failures.Failures
import failures.OrderFailures._
import failures.PromotionFailures._
import models.cord.OrderPromotions.scope._
import models.cord._
import models.cord.lineitems._
import CartLineItems.scope._
import models.coupon._
import models.discount.DiscountHelpers._
import models.discount._
import models.discount.offers._
import models.discount.qualifiers._
import models.objects._
import models.promotion.Promotions.scope._
import models.promotion._
import models.shipping
import models.account.User
import responses.TheResponse
import responses.cord.CartResponse
import services.discount.compilers._
import services.taxes.TaxesService
import services.{CartValidator, LogActivity}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.Apis
import utils.db._

object CartPromotionUpdater {

  def readjust(cart: Cart)(implicit ec: EC, es: ES, db: DB, ctx: OC): DbResultT[Unit] =
    for {
      // Fetch base stuff
      orderPromo ← * <~ OrderPromotions
                    .filterByCordRef(cart.refNum)
                    .requiresCoupon
                    .mustFindOneOr(OrderHasNoPromotions)
      // Fetch promotion
      promotion ← * <~ Promotions
                   .filterByContextAndShadowId(ctx.id, orderPromo.promotionShadowId)
                   .requiresCoupon
                   .mustFindOneOr(
                       PromotionShadowNotFoundForContext(orderPromo.promotionShadowId, ctx.id))
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
      promoObject = IlluminatedPromotion.illuminate(ctx, promotion, promoForm, promoShadow)
      _         ← * <~ promoObject.mustBeActive
      discounts ← * <~ PromotionDiscountLinks.queryRightByLeft(promotion)
      // Safe AST compilation
      discount ← * <~ tryDiscount(discounts)
      (form, shadow) = discount.tupled
      qualifier   ← * <~ QualifierAstCompiler(qualifier(form, shadow)).compile()
      offer       ← * <~ OfferAstCompiler(offer(form, shadow)).compile()
      adjustments ← * <~ getAdjustments(promoShadow, cart, qualifier, offer)
      // Delete previous adjustments and create new
      _ ← * <~ OrderLineItemAdjustments
           .filterByOrderRefAndShadow(cart.refNum, orderPromo.promotionShadowId)
           .delete
      _ ← * <~ OrderLineItemAdjustments.createAll(adjustments)
    } yield {}

  def attachCoupon(originator: User, refNum: Option[String] = None, code: String)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      apis: Apis): DbResultT[TheResponse[CartResponse]] =
    for {
      // Fetch base data
      cart ← * <~ getCartByOriginator(originator, refNum)
      orderPromotions ← * <~ OrderPromotions
                         .filterByCordRef(cart.refNum)
                         .requiresCoupon
                         .mustNotFindOneOr(OrderAlreadyHasCoupon)
      // Fetch coupon + validate
      couponCode ← * <~ CouponCodes.mustFindByCode(code)
      coupon ← * <~ Coupons
                .filterByContextAndFormId(ctx.id, couponCode.couponFormId)
                .mustFindOneOr(CouponWithCodeCannotBeFound(code))
      couponForm   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      couponShadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
      couponObject = IlluminatedCoupon.illuminate(ctx, coupon, couponForm, couponShadow)
      _ ← * <~ couponObject.mustBeActive
      _ ← * <~ couponObject.mustBeApplicable(couponCode, cart.accountId)
      // Fetch promotion + validate
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(ctx.id, coupon.promotionId)
                   .requiresCoupon
                   .mustFindOneOr(PromotionNotFoundForContext(coupon.promotionId, ctx.name))
      // Create connected promotion and line item adjustments
      _ ← * <~ OrderPromotions.create(OrderPromotion.buildCoupon(cart, promotion, couponCode))
      _ ← * <~ readjust(cart)
      // Write event to application logs
      _ ← * <~ LogActivity.orderCouponAttached(cart, couponCode)
      // Response
      tax         ← * <~ TaxesService.getTaxRate(cart)
      updatedCart ← * <~ CartTotaler.saveTotals(cart, tax)
      validated   ← * <~ CartValidator(updatedCart).validate()
      response    ← * <~ CartResponse.buildRefreshed(updatedCart)
    } yield TheResponse.validated(response, validated)

  def detachCoupon(originator: User, refNum: Option[String] = None)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      apis: Apis): DbResultT[TheResponse[CartResponse]] =
    for {
      // Read
      cart            ← * <~ getCartByOriginator(originator, refNum)
      orderPromotions ← * <~ OrderPromotions.filterByCordRef(cart.refNum).requiresCoupon.result
      shadowIds = orderPromotions.map(_.promotionShadowId)
      promotions ← * <~ Promotions.filter(_.shadowId.inSet(shadowIds)).requiresCoupon.result
      deleteShadowIds = promotions.map(_.shadowId)
      // Write
      _ ← * <~ OrderPromotions.filterByOrderRefAndShadows(cart.refNum, deleteShadowIds).delete
      _ ← * <~ OrderLineItemAdjustments
           .filterByOrderRefAndShadows(cart.refNum, deleteShadowIds)
           .delete
      tax         ← * <~ TaxesService.getTaxRate(cart)
      updatedCart ← * <~ CartTotaler.saveTotals(cart, tax)
      _           ← * <~ LogActivity.orderCouponDetached(updatedCart)
      validated   ← * <~ CartValidator(updatedCart).validate()
      response    ← * <~ CartResponse.buildRefreshed(updatedCart)
    } yield TheResponse.validated(response, validated)

  /**
    * Getting only first discount now
    */
  def tryDiscount[T](discounts: Seq[T]): Failures Xor T = discounts.headOption match {
    case Some(discount) ⇒ Xor.Right(discount)
    case _              ⇒ Xor.Left(EmptyDiscountFailure.single)
  }

  private def getAdjustments(promo: ObjectShadow, cart: Cart, qualifier: Qualifier, offer: Offer)(
      implicit ec: EC,
      es: ES,
      db: DB) =
    for {
      cartDetails ← * <~ fetchCartDetails(cart)
      (lineItems, shippingMethod) = cartDetails
      input                       = DiscountInput(promo, cart, lineItems, shippingMethod)
      _           ← * <~ qualifier.check(input)
      adjustments ← * <~ offer.adjust(input)
    } yield adjustments

  private def fetchCartDetails(cart: Cart)(implicit ec: EC) =
    for {
      lineItemTup ← CartLineItems.byCordRef(cart.refNum).lineItems.result
      lineItems = lineItemTup.map {
        case (sku, skuForm, skuShadow, productShadow, lineItem) ⇒
          CartLineItemProductData(sku, skuForm, skuShadow, productShadow, lineItem)
      }
      shipMethod ← shipping.ShippingMethods.forCordRef(cart.refNum).one
    } yield (lineItems, shipMethod)
}
