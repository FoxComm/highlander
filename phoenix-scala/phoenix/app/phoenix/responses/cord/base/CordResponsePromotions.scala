package phoenix.responses.cord.base

import cats.implicits._
import core.db._
import objectframework.models._
import phoenix.failures.CouponFailures._
import phoenix.failures.PromotionFailures.PromotionNotFound
import phoenix.models.cord.{OrderPromotion, OrderPromotions}
import phoenix.models.coupon._
import phoenix.models.discount.IlluminatedDiscount
import phoenix.models.objects.PromotionDiscountLinks
import phoenix.models.promotion.Promotions.scope._
import phoenix.models.promotion._
import phoenix.responses.CouponResponses.CouponResponse
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses.ResponseItem
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

case class CordResponseCouponPair(coupon: CouponResponse.Root, code: String) extends ResponseItem

object CordResponsePromotions {

  def fetch(cordRef: String)(implicit db: DB, ec: EC, ctx: OC): DbResultT[Option[CordResponsePromoDetails]] =
    for {
      orderPromo ← * <~ OrderPromotions.filterByCordRef(cordRef).one
      promo      ← * <~ fetchPromoDetails(orderPromo)
    } yield promo

  private def fetchPromoDetails(orderPromo: Option[OrderPromotion])(
      implicit db: DB,
      ec: EC,
      ctx: OC): DbResultT[Option[CordResponsePromoDetails]] = {
    // FIXME: how to compose this better without laziness? This is awful. :/ @michalrus
    val coupon    = orderPromo.flatTraverse(_.couponCodeId.traverse(fetchCoupon))
    lazy val auto = orderPromo.traverse(x ⇒ fetchAutoApply(x.promotionShadowId))
    lazyOrElse(fa = coupon.map(_.map { case (a, b) ⇒ (a, b.some) }), fb = auto.map(_.map((_, none))))
  }

  /** Try `fa` and if it’s `None`, evaluate and fallback to `fb`. Basically, `Option#orElse` lifted to `DbResultT`. */
  private def lazyOrElse[A](fa: DbResultT[Option[A]], fb: ⇒ DbResultT[Option[A]])(
      implicit ec: EC): DbResultT[Option[A]] =
    fa.flatMap(_.map(a ⇒ a.some.pure[DbResultT]).getOrElse(fb))

  private def renderPromotionResponse(
      promotion: Promotion)(implicit ec: EC, ctx: OC, db: DB): DbResultT[PromotionResponse.Root] =
    for {
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)

      discounts ← * <~ PromotionDiscountLinks.queryRightByLeft(promotion)
      // Illuminate
      theDiscounts = discounts.map(discount ⇒
        IlluminatedDiscount.illuminate(ctx.some, discount.form, discount.shadow))
      thePromotion = IlluminatedPromotion.illuminate(ctx, promotion, promoForm, promoShadow)
      // Responses
      respPromo = PromotionResponse.build(thePromotion, theDiscounts, promotion)
    } yield respPromo

  private def fetchAutoApply(
      promotionShadowId: Int)(implicit ec: EC, db: DB, ctx: OC): DbResultT[PromotionResponse.Root] =
    renderHistoricalPromotion(promotionShadowId)

  private def renderHistoricalPromotion(
      promotionShadowId: Int)(implicit ec: EC, db: DB, ctx: OC): DbResultT[PromotionResponse.Root] =
    for {
      promotionShadow ← * <~ ObjectShadows.mustFindById404(promotionShadowId)
      promotionFormId = promotionShadow.formId
      promotionForm ← * <~ ObjectForms.mustFindById404(promotionFormId)
      promotionHead ← * <~ Promotions
                       .filterByContext(ctx.id)
                       .filter(_.formId === promotionFormId)
                       .mustFindOneOr(PromotionNotFound(promotionFormId))

      illuminatedPromotion = IlluminatedPromotion
        .illuminate(ctx, promotionHead, promotionForm, promotionShadow)

      // FIXME: this is soooo very wrong @michalrus
      // FIXME: we’re returning **CURRENT** head discounts for a historical promotion… @michalrus
      // FIXME: https://foxcommerce.slack.com/archives/phoenix/p1489674138182180 @michalrus
      discounts ← * <~ PromotionDiscountLinks.queryRightByLeft(promotionHead)
      illuminatedDiscounts = discounts.map(discount ⇒
        IlluminatedDiscount.illuminate(ctx.some, discount.form, discount.shadow))
    } yield PromotionResponse.build(illuminatedPromotion, illuminatedDiscounts, promotionHead)

  // TBD: Get discounts from cached field in `OrderPromotion` model
  private def fetchCoupon(couponCodeId: Int)(
      implicit db: DB,
      ec: EC,
      ctx: OC): DbResultT[(PromotionResponse.Root, CordResponseCouponPair)] =
    for {
      // Coupon
      couponCode ← * <~ CouponCodes
                    .findOneById(couponCodeId)
                    .mustFindOr(CouponCodeNotFound(couponCodeId))
      coupon ← * <~ Coupons
                .filterByContextAndFormId(ctx.id, couponCode.couponFormId)
                .mustFindOneOr(CouponWithCodeCannotBeFound(couponCode.code))
      couponForm   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      couponShadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
      // Promotion
      // FIXME: very, very wrong @michalrus
      // FIXME: if that promotion got edited by an admin, we’ll get current version in OrderResponse @michalrus
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(ctx.id, coupon.promotionId)
                   .couponOnly
                   .mustFindOneOr(PromotionNotFound(coupon.promotionId))
      // Illuminate
      theCoupon = IlluminatedCoupon.illuminate(ctx, coupon, couponForm, couponShadow)
      // Responses
      respPromo ← * <~ renderPromotionResponse(promotion)
      respCoupon     = CouponResponse.build(theCoupon, couponCode.code, coupon)
      respCouponPair = CordResponseCouponPair(coupon = respCoupon, code = couponCode.code)
    } yield (respPromo, respCouponPair)

}
