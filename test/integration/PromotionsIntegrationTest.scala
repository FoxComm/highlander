import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.promotion._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import payloads.CouponPayloads._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads._
import responses.CouponResponses.CouponResponse
import responses.PromotionResponses.PromotionResponse
import services.coupon.CouponManager
import services.promotion.PromotionManager
import util._
import util.fixtures.BakedFixtures
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.time.RichInstant

class PromotionsIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "DELETE /v1/promotions/:context/:id" - {
    "archive existing promotion with attached coupons" in new Fixture {
      val response = DELETE(s"v1/promotions/${ctx.name}/${promotion.formId}")

      response.status must === (StatusCodes.OK)

      val promotionResponse = response.as[PromotionResponse.Root]
      withClue(promotionResponse.archivedAt.value → Instant.now) {
        promotionResponse.archivedAt.value.isBeforeNow === true
      }

      val couponResponse = GET(s"v1/coupons/${ctx.name}/${coupon.form.id}")
      val couponRoot     = couponResponse.as[CouponResponse.Root]
      withClue(couponRoot.archivedAt.value → Instant.now) {
        couponRoot.archivedAt.value.isBeforeNow === true
      }
    }

    "404 for not existing coupon" in new Fixture {
      val response = DELETE(s"v1/promotions/${ctx.name}/666")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Promotion, 666).description)
    }

    "404 when context not found" in new Fixture {
      val contextName = "donkeyContext"
      val response    = DELETE(s"v1/promotions/$contextName/${promotion.formId}")

      response.status must === (StatusCodes.NotFound)
      response.error must === (ObjectContextNotFound(contextName).description)
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val percentOff   = 10
    val totalAmount  = 0
    val discountForm = CreateDiscountForm(attributes = parse(s"""
    {
      "title" : "Get $percentOff% off when you spend $totalAmount dollars",
      "description" : "$percentOff% off when you spend over $totalAmount dollars",
      "tags" : [],
      "qualifier" : {
        "orderTotalAmount" : {
          "totalAmount" : ${totalAmount * 100}
        }
      },
      "offer" : {
        "orderPercentOff": {
          "discount": $percentOff
        }
      }
    }"""))
    val discountShadow = CreateDiscountShadow(
        attributes = parse("""
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "tags" : {"type": "tags", "ref": "tags"},
          "qualifier" : {"type": "qualifier", "ref": "qualifier"},
          "offer" : {"type": "offer", "ref": "offer"}
        }"""))

    val promoForm = CreatePromotionForm(attributes = ("name"
                                                → (("t" → "string") ~ ("v" → "donkey promo"))),
                                        discounts = Seq(discountForm))
    val promoShadow = CreatePromotionShadow(attributes = ("name"
                                                    → (("type" → "string") ~ ("ref" → "name"))),
                                            discounts = Seq(discountShadow))
    val promoPayload = CreatePromotion(applyType = Promotion.Coupon, promoForm, promoShadow)

    val couponForm = CreateCouponForm(attributes = ("name" → "donkey coupon"))

    val couponShadow = CreateCouponShadow(
        attributes = ("name"
                → (("type" → "string") ~ ("ref" → "name"))))

    def couponPayload(promoId: Int): CreateCoupon = CreateCoupon(couponForm, couponShadow, promoId)

    val (promotion, coupon) = (for {
      promoRoot ← * <~ PromotionManager.create(promoPayload, ctx.name)
      promotion ← * <~ Promotions
                   .filter(_.contextId === ctx.id)
                   .filter(_.formId === promoRoot.form.id)
                   .filter(_.shadowId === promoRoot.shadow.id)
                   .mustFindOneOr(NotFoundFailure404(Promotion, "test"))

      coupon ← * <~ CouponManager.create(couponPayload(promoRoot.form.id), ctx.name, None)
    } yield (promotion, coupon)).gimme
  }

}
