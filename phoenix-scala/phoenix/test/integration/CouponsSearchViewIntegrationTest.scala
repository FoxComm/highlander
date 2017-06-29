import java.time.Instant

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import objectframework.ObjectUtils
import org.json4s._
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import phoenix.models.account.Scope
import phoenix.models.image._
import phoenix.models.objects._
import phoenix.models.product._
import phoenix.payloads.ImagePayloads._
import phoenix.responses.SkuResponses.SkuResponse
import phoenix.services.image.ImageManager
import phoenix.utils.aliases.Json
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.utils.Money.Currency
import core.db._
import org.json4s.JsonAST.{JField, JNull, JObject}
import phoenix.models.promotion.Promotion
import phoenix.models.returns.ReturnLineItem
import phoenix.models.returns.ReturnLineItem.OriginType
import phoenix.payloads.CouponPayloads.{CreateCoupon, GenerateCouponCodes}
import phoenix.responses.CouponResponses.CouponResponse
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.utils.JsonFormatters
import testutils.PayloadHelpers.tv
import testutils.fixtures.api.PromotionPayloadBuilder
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}

object CouponsSearchViewIntegrationTest {

  case class CouponSearchViewResult(
      id: Int,
      promotionId: Int,
      context: String,
      name: String,
      storefrontName: String,
      description: String,
      activeFrom: Json,
      activeTo: Json,
      totalUsed: Int,
      createdAt: Json,
      archivedAt: Option[Json],
      scope: LTree,
      codes: Json,
      maxUsesPerCode: Int,
      maxUsesPerCustomer: Int
  )

}

class CouponsSearchViewIntegrationTest
    extends SearchViewTestBase
    with IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures
    with SkuOps {

  import CouponsSearchViewIntegrationTest._

  type SearchViewResult = CouponSearchViewResult
  val searchViewName: String = "coupons_search_view"
  val searchKeyName: String  = "id"

  "Each coupon code should have a corresponsing search_view row" in {
    val percentOff = 37
    val prefix     = "BESTPRICE4U"
    val quantity   = 20

    val promo = promotionsApi
      .create(PromotionPayloadBuilder
        .build(Promotion.Coupon, PromoOfferBuilder.CartPercentOff(percentOff), PromoQualifierBuilder.CartAny))
      .as[PromotionResponse]

    val coupons = {
      val couponPayload = {
        val usageRules = Map("isExclusive" → false,
                             "isUnlimitedPerCode"     → false,
                             "usesPerCode"            → 1,
                             "isUnlimitedPerCustomer" → false,
                             "usesPerCustomer"        → 1)

        val attrs = Map(
          "usageRules"     → tv(usageRules, "usageRules"),
          "name"           → tv("testyCoupon"),
          "storefrontName" → tv("<p>Testy coupon</p>", "richText"),
          "activeFrom"     → tv(Instant.now, "datetime"),
          "activeTo"       → tv(JNull, "datetime")
        )
        CreateCoupon(promotion = promo.id,
                     attributes = attrs,
                     singleCode = None,
                     generateCodes =
                       Some(GenerateCouponCodes(prefix, quantity = quantity, length = prefix.length + 3)))
      }
      couponsApi.create(couponPayload).as[Seq[CouponResponse]]
    }

    // one Coupon is created for each CouponCode
    coupons.size must === (quantity)

    // check if each coupon has a corresponding view row
    coupons.foreach { coupon ⇒
      findOne(coupon.id) mustBe 'defined
    }
  }
}
