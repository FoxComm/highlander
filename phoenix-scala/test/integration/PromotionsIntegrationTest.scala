import java.time.Instant

import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.objects.ObjectContext
import models.promotion._
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import payloads.CouponPayloads._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads._
import responses.CouponResponses.CouponResponse
import responses.PromotionResponses.PromotionResponse
import services.coupon.CouponManager
import services.promotion.PromotionManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.PayloadHelpers._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.time.RichInstant
import utils.aliases._

class PromotionsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "DELETE /v1/promotions/:context/:id" - {
    "archive existing promotion with attached coupons" in new Fixture {
      val promotionResponse = promotionsApi(promotion.formId).delete().as[PromotionResponse.Root]

      withClue(promotionResponse.archivedAt.value → Instant.now) {
        promotionResponse.archivedAt.value.isBeforeNow mustBe true
      }

      val couponRoot = couponsApi(coupon.id).get().as[CouponResponse.Root]
      withClue(couponRoot.archivedAt.value → Instant.now) {
        couponRoot.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "404 for not existing coupon" in new Fixture {
      promotionsApi(666).delete().mustFailWith404(NotFoundFailure404(Promotion, 666))
    }

    "404 when context not found" in new Fixture {
      implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)
      promotionsApi(promotion.formId)(donkeyContext)
        .delete()
        .mustFailWith404(ObjectContextNotFound("donkeyContext"))
    }
  }

  "PATCH /v1/promotions/:context/:id" - {
    "change qualifier in promotion" in new Fixture {

      val newDiscountAttrs = makeDiscountAttrs("orderAny", "any" → JInt(0))
      val formDiscount     = promoRoot.discounts.head
      val disablePromoPayload =
        UpdatePromotion(applyType = promotion.applyType,
                        attributes = promoAttributes,
                        discounts =
                          Seq(UpdatePromoDiscount(formDiscount.id, attributes = newDiscountAttrs)))

      promotionsApi(promotion.formId).update(disablePromoPayload).as[PromotionResponse.Root]
    }
  }

  trait Fixture extends StoreAdmin_Seed {

    implicit val au = storeAdminAuthData

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

    val (promotion, coupon, promoRoot) = (for {
      promoRoot ← * <~ PromotionManager.create(promoPayload, ctx.name)
      promotion ← * <~ Promotions
                   .filter(_.contextId === ctx.id)
                   .filter(_.formId === promoRoot.id)
                   .mustFindOneOr(NotFoundFailure404(Promotion, "test"))

      coupon ← * <~ CouponManager.create(couponPayload(promoRoot.id), ctx.name, None)
    } yield (promotion, coupon, promoRoot)).gimme
  }

}
