package testutils.fixtures.api

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import scala.util.Random
import faker.Lorem
import models.promotion.Promotion
import org.json4s.JsonDSL._
import org.scalatest.SuiteMixin
import payloads.CouponPayloads.CreateCoupon
import payloads.ProductPayloads.CreateProductPayload
import payloads.SkuPayloads.SkuPayload
import responses.CouponResponses.CouponResponse
import responses.ProductResponses.ProductResponse.{Root ⇒ ProductRoot}
import responses.PromotionResponses.PromotionResponse
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}
import utils.aliases.Json

trait ApiFixtures extends SuiteMixin with HttpSupport with PhoenixAdminApi { self: FoxSuite ⇒

  trait ProductSku_ApiFixture {
    private val productCode = s"testprod_${Lorem.numerify("####")}"
    val skuCode             = s"$productCode-sku_${Lorem.letterify("????").toUpperCase}"
    def skuPrice: Int = Random.nextInt(20000) + 100

    private val skuPayload = SkuPayload(
        attributes = Map("code"        → tv(skuCode),
                         "title"       → tv(skuCode.capitalize),
                         "salePrice"   → tv(("currency" → "USD") ~ ("value" → skuPrice), "price"),
                         "retailPrice" → tv(("currency" → "USD") ~ ("value" → skuPrice), "price")))

    val productPayload =
      CreateProductPayload(
          attributes =
            Map("name" → tv(productCode.capitalize), "title" → tv(productCode.capitalize)),
          slug = productCode.toLowerCase,
          skus = Seq(skuPayload),
          variants = None)

    val product = productsApi.create(productPayload).as[ProductRoot]
  }

  trait Coupon_AnyQualifier_PercentOff extends CouponFixtureBase {
    def percentOff: Int = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
        Promotion.Coupon,
        PromoOfferBuilder.CartPercentOff(percentOff),
        PromoQualifierBuilder.CartAny)

    def promotion = promotionsApi.create(promoPayload).as[PromotionResponse.Root]
  }

  trait Coupon_TotalQualifier_PercentOff extends CouponFixtureBase {
    def qualifiedSubtotal: Int = 5000
    def percentOff: Int        = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
        Promotion.Coupon,
        PromoOfferBuilder.CartPercentOff(percentOff),
        PromoQualifierBuilder.CartTotalAmount(qualifiedSubtotal))

    def promotion = promotionsApi.create(promoPayload).as[PromotionResponse.Root]
  }

  trait Coupon_NumItemsQualifier_PercentOff extends CouponFixtureBase {
    def qualifiedNumItems: Int = 5
    def percentOff: Int        = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
        Promotion.Coupon,
        PromoOfferBuilder.CartPercentOff(percentOff),
        PromoQualifierBuilder.CartNumUnits(qualifiedNumItems))

    lazy val promotion = promotionsApi.create(promoPayload).as[PromotionResponse.Root]
  }

  trait CouponFixtureBase {
    def couponActiveFrom: Instant       = Instant.now.minus(1, DAYS)
    def couponActiveTo: Option[Instant] = None

    def promotion: PromotionResponse.Root

    lazy val coupon = couponsApi
      .create(CreateCoupon(couponAttrs(couponActiveFrom, couponActiveTo), promotion.id))
      .as[CouponResponse.Root]

    lazy val couponCode = couponsApi(coupon.id).codes.generate(Lorem.letterify("?????")).as[String]

    protected def couponAttrs(activeFrom: Instant, activeTo: Option[Instant]): Map[String, Json] = {
      val usageRules = {
        ("isExclusive"            → true) ~
        ("isUnlimitedPerCode"     → true) ~
        ("isUnlimitedPerCustomer" → true)
      }.asShadowVal(t = "usageRules")

      val commonAttrs = Map[String, Any]("name" → "Order coupon",
                                         "storefrontName" → "Order coupon",
                                         "description"    → "Order coupon description",
                                         "details"        → "Order coupon details".richText,
                                         "usageRules"     → usageRules,
                                         "activeFrom"     → activeFrom)

      activeTo.fold(commonAttrs)(act ⇒ commonAttrs + ("activeTo" → act)).asShadow
    }

  }
}
