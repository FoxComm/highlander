package testutils.fixtures.api

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import faker.Lorem
import org.json4s.JsonDSL._
import org.scalatest.SuiteMixin
import payloads.CouponPayloads.CreateCoupon
import responses.CouponResponses.CouponResponse
import responses.ProductResponses.ProductResponse.{Root ⇒ ProductRoot}
import responses.ProductVariantResponses.ProductVariantResponse.{Root ⇒ VariantRoot}
import responses.PromotionResponses.PromotionResponse
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.PromotionPayloadBuilder._
import testutils.fixtures.api.products._
import utils.aliases.Json

trait ApiFixtures extends SuiteMixin with HttpSupport with PhoenixAdminApi { self: FoxSuite ⇒

  trait ProductVariant_ApiFixture {
    def productVariantPrice: Int = 20000

    val payloadBuilder: InvariantProductPayloadBuilder = InvariantProductPayloadBuilder(
        price = productVariantPrice)

    val product: ProductRoot = productsApi.create(payloadBuilder.createPayload).as[ProductRoot]

    val productVariant: VariantRoot = product.variants.onlyElement
    val productVariantCode: String  = productVariant.attributes.code
  }

  // Generates all possible variant codes and attaches them to all appropriate options
  // To change the behavior, provide a `def` override defaulting to `AllVariantsCfg`
  trait Product_ColorSizeOptions_ApiFixture {
    val productName: String = randomProductName

    def price: Int = 20000

    object sizes {
      val small: String    = "small"
      val large: String    = "large"
      val all: Seq[String] = Seq(small, large)
    }

    object colors {
      val red: String      = "red"
      val green: String    = "green"
      val all: Seq[String] = Seq(red, green)
    }

    val payloadBuilder: TwoOptionProductPayloadBuilder = TwoOptionProductPayloadBuilder(
        ProductOptionCfg(name = "Size", values = sizes.all),
        ProductOptionCfg(name = "Color", values = colors.all),
        AllVariantsCfg,
        AllVariantsCfg,
        price,
        productName)

    val variantsQty: Int = payloadBuilder.createProductPayload.variants.length
    val optionsQty: Int  = payloadBuilder.createProductPayload.options.map(_.length).getOrElse(0)

    val product: ProductRoot =
      productsApi.create(payloadBuilder.createProductPayload).as[ProductRoot]
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
}
