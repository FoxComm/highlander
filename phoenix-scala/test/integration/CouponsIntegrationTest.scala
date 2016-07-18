import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import failures.ObjectFailures.ObjectContextNotFound
import models.StoreAdmins
import models.coupon.Coupon
import models.objects.{ObjectContext, ObjectContexts}
import models.product.SimpleContext
import models.promotion.Promotion
import org.json4s.JsonDSL._
import payloads.CouponPayloads._
import payloads.PromotionPayloads._
import responses.CouponResponses.CouponResponse
import services.coupon.CouponManager
import services.promotion.PromotionManager
import util.{IntegrationTestBase, TestActivityContext}
import utils.db._
import utils.time.RichInstant

class CouponsIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC {

  "POST /v1/coupons/:context/:id/archive" - {
    "archive existing coupon" in new Fixture {
      val response = POST(s"v1/coupons/${context.name}/${coupon.form.id}/archive")

      response.status must === (StatusCodes.OK)

      val couponResponse = response.as[CouponResponse.Root]
      withClue(couponResponse.archivedAt.value → Instant.now) {
        couponResponse.archivedAt.value.isBeforeNow === true
      }
    }

    "404 for not existing coupon" in new Fixture {
      val response = POST(s"v1/coupons/${context.name}/666/archive")

      response.status must === (StatusCodes.NotFound)
      response.error === (NotFoundFailure404(Coupon, 666).description)
    }

    "404 when context not found" in new Fixture {
      val contextName = "donkeyContext"
      val response    = POST(s"v1/coupons/$contextName/${coupon.form.id}/archive")

      response.status must === (StatusCodes.NotFound)
      response.error === (NotFoundFailure404(ObjectContext, contextName).description)
    }
  }

  trait Fixture {
    val promoForm = CreatePromotionForm(attributes = ("name"
                                                → (("t" → "string") ~ ("v" → "donkey promo"))),
                                        discounts = Seq.empty)
    val promoShadow = CreatePromotionShadow(attributes = ("name"
                                                    → (("type" → "string") ~ ("ref" → "name"))),
                                            discounts = Seq.empty)
    val promoPayload = CreatePromotion(applyType = Promotion.Coupon, promoForm, promoShadow)

    val couponForm = CreateCouponForm(
        attributes = ("name"
                → (("t" → "string") ~ ("v" → "donkey coupon"))))
    val couponShadow = CreateCouponShadow(
        attributes = ("name"
                → (("type" → "string") ~ ("ref" → "name"))))

    def couponPayload(promoId: Int): CreateCoupon = CreateCoupon(couponForm, couponShadow, promoId)

    val (storeAdmin, context, coupon) = (for {
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))
      promotion ← * <~ PromotionManager.create(promoPayload, context.name)
      coupon    ← * <~ CouponManager.create(couponPayload(promotion.form.id), context.name, None)
    } yield (storeAdmin, context, coupon)).gimme
  }

}
