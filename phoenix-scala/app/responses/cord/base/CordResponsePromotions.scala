package responses.cord.base

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
import slick.dbio.DBIO
import utils.aliases._
import utils.db._

case class CordResponseCouponPair(coupon: CouponResponse.Root, code: String) extends ResponseItem

object CordResponsePromotions {

  def fetch(cordRef: String)(implicit db: DB, ec: EC, ctx: OC): DBIO[CordResponsePromoDetails] =
    for {
      orderPromo ← OrderPromotions.filterByCordRef(cordRef).one
      promo      ← fetchPromoDetails(orderPromo)
    } yield promo

  private def fetchPromoDetails(orderPromo: Option[OrderPromotion])(
      implicit db: DB,
      ec: EC,
      ctx: OC): DBIO[CordResponsePromoDetails] =
    orderPromo match {
      case Some(op) ⇒
        fetchCouponDetails(op.couponCodeId) // TBD: Handle auto-apply promos here later
      case _ ⇒
        DBIO.successful(None)
    }

  private def fetchCouponDetails(couponCodeId: Option[Int])(
      implicit db: DB,
      ec: EC,
      ctx: OC): DBIO[CordResponsePromoDetails] =
    couponCodeId match {
      case Some(codeId) ⇒
        fetchCoupon(codeId).fold(_ ⇒ None, good ⇒ good)
      case _ ⇒
        DBIO.successful(None)
    }

  // TBD: Get discounts from cached field in `OrderPromotion` model
  private def fetchCoupon(couponCodeId: Int)(implicit db: DB, ec: EC, ctx: OC) =
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
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)

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
    } yield (respPromo, respCouponPair).some

}
