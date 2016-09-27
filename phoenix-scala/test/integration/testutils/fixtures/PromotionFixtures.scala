package testutils.fixtures

import failures.NotFoundFailure404
import models.account.User
import models.coupon.Coupons
import models.objects.ObjectUtils
import models.promotion.{Promotion, Promotions}
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import payloads.CouponPayloads._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads.CreatePromotion
import responses.PromotionResponses.PromotionResponse
import services.Authenticator.AuthData
import services.coupon.CouponManager
import services.promotion.PromotionManager
import testutils.PayloadHelpers._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

trait PromotionFixtures extends TestFixtureBase {

  trait Promotion_Seed {
    implicit def au: AU

    def makeDiscountAttrs(qualifier: String, qualifierValue: JObject): Map[String, Json] = {
      Map[String, Any](
          "title"       → s"Get $percentOff% off when you spend $totalAmount dollars",
          "description" → s"$percentOff% off when you spend over $totalAmount dollars",
          "tags"        → tv(JArray(List.empty[JString]), "tags"),
          "qualifier"   → JObject(qualifier → qualifierValue).asShadowVal(t = "qualifier"),
          "offer" → JObject(
              "orderPercentOff" → JObject(
                  "discount" → JInt(percentOff)
              )
          ).asShadowVal("offer")
      ).asShadow
    }

    val percentOff  = 10
    val totalAmount = 0
    val discountAttributes =
      makeDiscountAttrs("orderTotalAmount", "totalAmount" → JInt(totalAmount * 100))

    val promoAttributes = Map[String, Json]("name" → tv("donkey promo"))

    val promoPayload = CreatePromotion(applyType = Promotion.Coupon,
                                       attributes = promoAttributes,
                                       discounts =
                                         Seq(CreateDiscount(attributes = discountAttributes)))

    val couponAttributes = Map[String, Json]("name" → tv("donkey coupon"))

    def couponPayload(promoId: Int): CreateCoupon =
      CreateCoupon(attributes = couponAttributes, promoId)

    val (promoRoot: PromotionResponse.Root, promotion: Promotion) = _createPromotionFromPayload(
        promoPayload)

    def _createPromotionFromPayload(promoPayload: CreatePromotion) = {
      (for {
        promoRoot ← * <~ PromotionManager.create(promoPayload, ctx.name)
        promotion ← * <~ Promotions
                     .filter(_.contextId === ctx.id)
                     .filter(_.formId === promoRoot.id)
                     .mustFindOneOr(NotFoundFailure404(Promotion, "test"))

        coupon ← * <~ CouponManager.create(couponPayload(promoRoot.id), ctx.name, None)
      } yield (promoRoot, promotion)).gimme
    }

  }

  trait Coupon_Raw {
    implicit def au: AuthData[User]

    def promotion: Promotion
    val coupon = _coupon
    lazy val fullCoupon = ObjectUtils
      .getFullObject(
          Coupons
            .filterByContextAndFormId(ctx.id, coupon.id)
            .mustFindOneOr(NotFoundFailure404(s"cannot find coupon with form id = ${coupon.id}")))
      .gimme

    def _couponPayload(promoId: Int): CreateCoupon = {
      CreateCoupon(attributes = Map("name" → JString("donkey coupon")), promoId)
    }

    def _coupon = CouponManager.create(_couponPayload(promotion.formId), ctx.name, None).gimme
  }

}
