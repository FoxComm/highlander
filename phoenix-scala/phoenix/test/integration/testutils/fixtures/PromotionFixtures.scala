package testutils.fixtures

import core.failures.NotFoundFailure404
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import phoenix.models.account.User
import phoenix.models.promotion.{Promotion, Promotions}
import phoenix.payloads.CouponPayloads._
import phoenix.payloads.DiscountPayloads._
import phoenix.payloads.PromotionPayloads.CreatePromotion
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.services.Authenticator.AuthData
import phoenix.services.coupon.CouponManager
import phoenix.services.promotion.PromotionManager
import phoenix.utils.aliases._
import testutils.PayloadHelpers._
import core.db.ExPostgresDriver.api._
import core.db._

trait PromotionFixtures extends TestFixtureBase {

  trait Promotion_Seed {
    implicit def au: AU

    def makeDiscountAttrs(qualifier: String, qualifierValue: JObject): Map[String, Json] =
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

    val percentOff  = 10
    val totalAmount = 0
    val discountAttributes =
      makeDiscountAttrs("orderTotalAmount", "totalAmount" → JInt(totalAmount * 100))

    val promoAttributes = Map[String, Json]("name" → tv("donkey promo"))

    val promoPayload = CreatePromotion(applyType = Promotion.Coupon,
                                       attributes = promoAttributes,
                                       discounts = Seq(CreateDiscount(attributes = discountAttributes)))

    val (promoRoot: PromotionResponse.Root, promotion: Promotion) = createPromotionFromPayload(promoPayload)

    def createPromotionFromPayload(promoPayload: CreatePromotion) =
      (for {
        promoRoot ← * <~ PromotionManager.create(promoPayload, ctx.name, None)
        promotion ← * <~ Promotions
                     .filter(_.contextId === ctx.id)
                     .filter(_.formId === promoRoot.id)
                     .mustFindOneOr(NotFoundFailure404(Promotion, "test"))
      } yield (promoRoot, promotion)).gimme

  }

  trait Coupon_Raw {
    implicit def au: AuthData[User]

    def promotion: Promotion
    val coupon = CouponManager.create(couponPayload(promotion.formId), ctx.name, None).gimme

    def couponPayload(promoId: Int, attributes: Map[String, Json] = Map()): CreateCoupon =
      CreateCoupon(attributes = attributes + ("name" → tv("donkey coupon")), promoId)
  }

}
