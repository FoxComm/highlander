package responses.cord.base

import cats._
import cats.implicits._
import failures.CouponFailures._
import failures.PromotionFailures.PromotionNotFound
import models.cord.{OrderPromotion, OrderPromotions}
import models.coupon._
import models.discount.IlluminatedDiscount
import models.objects._
import models.promotion.Promotions.scope._
import models.promotion._
import responses.CouponResponses.CouponResponse
import responses.PromotionResponses.PromotionResponse
import responses.ResponseItem
import utils.aliases._
import utils.db._

case class CordResponseCouponPair(coupon: CouponResponse.Root, code: String) extends ResponseItem

object CordResponsePromotions {

  def fetch(cordRef: String)(implicit db: DB,
                             ec: EC,
                             ctx: OC): DbResultT[Option[CordResponsePromoDetails]] =
    for {
      orderPromo ← * <~ OrderPromotions.filterByCordRef(cordRef).one
      promo      ← * <~ fetchPromoDetails(orderPromo)
    } yield promo

  private def fetchPromoDetails(orderPromo: Option[OrderPromotion])(
      implicit db: DB,
      ec: EC,
      ctx: OC): DbResultT[Option[CordResponsePromoDetails]] = {
    // FIXME: how to compose this better without laziness? This is awful. :/ @michalrus
    val coupon = orderPromo.traverseM(
        _.couponCodeId.traverse(x ⇒ fetchCoupon(x).map { case (a, b) ⇒ (a, Some(b)) }))
    lazy val auto = orderPromo.traverse(x ⇒
          fetchAutoApply(x.promotionShadowId).map((_, Option.empty[CordResponseCouponPair])))
    coupon.flatMap(_.fold(auto)(x ⇒ DbResultT.pure(x.some)))
  }

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

  private def fetchAutoApply(promotionShadowId: Int)(implicit ec: EC,
                                                     db: DB,
                                                     ctx: OC): DbResultT[PromotionResponse.Root] =
    for {
      // Promotion
      promotion ← * <~ Promotions
                   .filterByContextAndShadowId(ctx.id, promotionShadowId)
                   .autoApplied
                   .mustFindOneOr(PromotionNotFound(promotionShadowId))
      resp ← renderPromotionResponse(promotion)
    } yield resp

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
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(ctx.id, coupon.promotionId)
                   .requiresCoupon
                   .mustFindOneOr(PromotionNotFound(coupon.promotionId))
      // Illuminate
      theCoupon = IlluminatedCoupon.illuminate(ctx, coupon, couponForm, couponShadow)
      // Responses
      respPromo ← renderPromotionResponse(promotion)
      respCoupon     = CouponResponse.build(theCoupon, coupon)
      respCouponPair = CordResponseCouponPair(coupon = respCoupon, code = couponCode.code)
    } yield (respPromo, respCouponPair)

}
