package testutils.fixtures.api

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import scala.util.Random

import faker.Lorem
import org.json4s.JsonDSL._
import org.scalatest.SuiteMixin
import payloads.CouponPayloads.CreateCoupon
import payloads.ProductPayloads.CreateProductPayload
import payloads.ProductVariantPayloads.ProductVariantPayload
import responses.CouponResponses.CouponResponse
import responses.ProductResponses.ProductResponse.{Root ⇒ ProductRoot}
import responses.PromotionResponses.PromotionResponse
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.ProductPayloadBuilder._
import testutils.fixtures.api.PromotionPayloadBuilder._
import utils.aliases.Json

trait ApiFixtures extends SuiteMixin with HttpSupport with PhoenixAdminApi { self: FoxSuite ⇒

  trait ProductVariant_ApiFixture {
    val productName: String        = randomProductCode
    val productVariantCode: String = randomVariantCode(productName)

    def productVariantPrice: Int = Random.nextInt(20000) + 100

    private val variantPayload = ProductVariantPayload(
        attributes = variantAttrs(productVariantCode, productVariantPrice)
    )

    val productPayload: CreateProductPayload = CreateProductPayload(attributes =
                                                                      productAttrs(productName),
                                                                    slug = productName.toLowerCase,
                                                                    variants = Seq(variantPayload),
                                                                    options = None)

    val product: ProductRoot = productsApi.create(productPayload).as[ProductRoot]
  }

  trait Product_ColorSizeOptions_ApiFixture {
    val productCode: String = randomProductCode

    def price: Int = 2000

    private val payload = ProductPayloadBuilder.build(
        ProductOptionCfg(name = "Size", values = Seq("small", "large")),
        ProductOptionCfg(name = "Color", values = Seq("red", "green")),
        price,
        productCode)

    productsApi.create(payload).as[ProductRoot]
  }

  trait Coupon_TotalQualifier_PercentOff extends CouponFixtureBase {
    def qualifiedSubtotal: Int = 5000
    def percentOff: Int        = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
        PromoOfferBuilder.CartPercentOff(percentOff),
        PromoQualifierBuilder.CartTotalAmount(qualifiedSubtotal))

    def promotion = promotionsApi.create(promoPayload).as[PromotionResponse.Root]
  }

  trait Coupon_NumItemsQualifier_PercentOff extends CouponFixtureBase {
    def qualifiedNumItems: Int = 5
    def percentOff: Int        = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
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

  private def randomProductCode: String =
    s"prod_${Lorem.numerify("####")}"

  private def randomVariantCode(productCode: String): String =
    s"$productCode-${Lorem.letterify("????").toUpperCase}"
}
