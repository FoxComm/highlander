import java.time.Instant

import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.objects.{ObjectContext, ObjectUtils}
import models.promotion.Promotion.{Auto, Coupon}
import models.promotion._
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import payloads.PromotionPayloads._
import responses.CouponResponses.CouponResponse
import responses.PromotionResponses.PromotionResponse
import services.promotion.PromotionManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.{BakedFixtures, PromotionFixtures}
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._
import utils.time.RichInstant

class PromotionsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures
    with PromotionFixtures {

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

  "promotion with 'coupon' apply type should be active on" - {

    "creation" in new StoreAdmin_Seed with Promotion_Seed {
      def au: AU = storeAdminAuthData

      ObjectUtils
        .getFullObject(DbResultT.pure(promotion))
        .gimme
        .getAttribute("activeFrom") must !==(JNothing)
    }

    "updating" in new AutoApplyPromotionSeed {

      var fullPromotion = ObjectUtils.getFullObject(DbResultT.pure(promotion)).gimme
      fullPromotion.getAttribute("activeFrom") must === (JNothing)

      val attributes: List[(String, JValue)] =
        IlluminateAlgorithm.projectAttributes(fullPromotion.form.attributes,
                                              fullPromotion.shadow.attributes) match {
          case JObject(f) ⇒ f
          case _          ⇒ List()
        }

      PromotionManager
        .update(promotion.formId, UpdatePromotion(Coupon, attributes.toMap, Seq()), ctx.name)
        .gimme

      fullPromotion =
        ObjectUtils.getFullObject(Promotions.mustFindById400(fullPromotion.model.id)).gimme
      fullPromotion.getAttribute("activeFrom") must !==(JNothing)
    }
  }

  trait Fixture extends StoreAdmin_Seed with Promotion_Seed with Coupon_Raw {
    def au: AU = storeAdminAuthData
  }

  trait AutoApplyPromotionSeed extends StoreAdmin_Seed with Promotion_Seed {
    def au: AU = storeAdminAuthData

    override def createPromotionFromPayload(payload: CreatePromotion) =
      super.createPromotionFromPayload(payload.copy(applyType = Auto))
  }

}
