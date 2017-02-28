package responses.cord.base

import cats.implicits._
import models.cord.OrderPromotions
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

  // TODO: Handle auto-apply promos
  def fetch(
      cordRef: String)(implicit db: DB, ec: EC, ctx: OC): DbResultT[CordResponsePromoDetails] =
    for {
      orderPromo ← * <~ OrderPromotions.filterByCordRef(cordRef).one
      promo      ← * <~ orderPromo.flatMap(_.couponCodeId.map(fetchCoupon(_)))
    } yield promo

  // TBD: Get discounts from cached field in `OrderPromotion` model
  private def fetchCoupon(couponCodeId: Int)(implicit db: DB, ec: EC, ctx: OC) =
    for {
      // Coupon
      couponCode   ← * <~ CouponCodes.findById(couponCodeId)
      coupon       ← * <~ Coupons.filterByContextAndFormId(ctx.id, couponCode.couponFormId)
      couponForm   ← * <~ ObjectForms.findById(coupon.formId)
      couponShadow ← * <~ ObjectShadows.findById(coupon.shadowId)
      // Promotion
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(ctx.id, coupon.promotionId)
                   .requiresCoupon
      promoForm   ← * <~ ObjectForms.findById(promotion.formId)
      promoShadow ← * <~ ObjectShadows.findById(promotion.shadowId)

      discounts ← * <~ PromotionDiscountLinks.queryRightByLeft(promotion)
      // Illuminate
      theCoupon = IlluminatedCoupon.illuminate(ctx, coupon, couponForm, couponShadow)
      theDiscounts = discounts.map(discount ⇒
            IlluminatedDiscount.illuminate(ctx.some, discount.form, discount.shadow))
      thePromotion = IlluminatedPromotion.illuminate(ctx, promotion, promoForm, promoShadow)
      // Responses
      respPromo      = PromotionResponse.build(thePromotion, theDiscounts, promotion)
      respCoupon     = CouponResponse.build(theCoupon, coupon)
      respCouponPair = CordResponseCouponPair(coupon = respCoupon, code = couponCode.code)
    } yield (respPromo, respCouponPair)

}
