package testutils.fixtures

import failures.NotFoundFailure404
import io.circe.Json
import models.account.User
import models.promotion.{Promotion, Promotions}
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

    def makeDiscountAttrs(qualifier: String, qualifierValue: Json): Map[String, Json] = {
      Map(
          "title"       → tv(s"Get $percentOff% off when you spend $totalAmount dollars"),
          "description" → s"$percentOff% off when you spend over $totalAmount dollars",
          "tags"        → tv(Json.arr(), "tags"),
          "qualifier"   → Json.obj(qualifier → qualifierValue).asShadowVal(t = "qualifier"),
          "offer" → Json.obj(
              "orderPercentOff" → Json.obj(
                  "discount" → Json.fromInt(percentOff)
              )
          ).asShadowVal("offer")
      ).asShadow
    }

    val percentOff  = 10
    val totalAmount = 0
    val discountAttributes =
      makeDiscountAttrs("orderTotalAmount", Json.obj("totalAmount" → Json.fromInt(totalAmount * 100)))

    val promoAttributes = Map[String, Json]("name" → tv("donkey promo"))

    val promoPayload = CreatePromotion(applyType = Promotion.Coupon,
                                       attributes = promoAttributes,
                                       discounts =
                                         Seq(CreateDiscount(attributes = discountAttributes)))

    val (promoRoot: PromotionResponse.Root, promotion: Promotion) = createPromotionFromPayload(
        promoPayload)

    def createPromotionFromPayload(promoPayload: CreatePromotion) = {
      (for {
        promoRoot ← * <~ PromotionManager.create(promoPayload, ctx.name, None)
        promotion ← * <~ Promotions
                     .filter(_.contextId === ctx.id)
                     .filter(_.formId === promoRoot.id)
                     .mustFindOneOr(NotFoundFailure404(Promotion, "test"))
      } yield (promoRoot, promotion)).gimme
    }

  }

  trait Coupon_Raw {
    implicit def au: AuthData[User]

    def promotion: Promotion
    val coupon = CouponManager.create(couponPayload(promotion.formId), ctx.name, None).gimme

    def couponPayload(promoId: Int, attributes: Map[String, Json] = Map()): CreateCoupon = {
      CreateCoupon(attributes = attributes + ("name" → tv("donkey coupon")), promoId)
    }
  }

}
