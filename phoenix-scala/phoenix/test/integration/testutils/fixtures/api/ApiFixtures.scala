package testutils.fixtures.api

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import cats.data.NonEmptyList
import faker.Lorem
import org.json4s.JsonAST.JNull
import org.json4s.JsonDSL._
import org.scalatest.SuiteMixin
import phoenix.models.catalog.Catalog
import phoenix.models.coupon.CouponUsageRules
import phoenix.models.promotion.Promotion
import phoenix.payloads.CouponPayloads.CreateCoupon
import phoenix.payloads.ProductPayloads.CreateProductPayload
import phoenix.payloads.ProductReviewPayloads.{CreateProductReviewByCustomerPayload, CreateProductReviewPayload}
import phoenix.payloads.SkuPayloads.SkuPayload
import phoenix.payloads.CatalogPayloads._
import phoenix.responses.CatalogResponse
import phoenix.responses.CouponResponses.CouponResponse
import phoenix.responses.ProductResponses.ProductResponse.{Root ⇒ ProductRoot}
import phoenix.responses.ProductReviewResponses.ProductReviewResponse
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses.SkuResponses.SkuResponse
import phoenix.utils.aliases.Json
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}

import scala.util.Random
import phoenix.payloads.VariantPayloads.{VariantPayload, VariantValuePayload}

trait ApiFixtures extends SuiteMixin with HttpSupport with PhoenixAdminApi with JwtTestAuth { self: FoxSuite ⇒

  /** Transitions through list of states.
    *
    * If `initial` is empty it will transition through all of the passed `transitionStates`.
    * If it's defined it will skip any states in `transitionStates` until `initial` is found.
    */
  def transitionEntity[S, E](transitionStates: NonEmptyList[S], initial: Option[E])(getState: E ⇒ S)(
      updateState: S ⇒ E): E = {
    val transition = initial
      .map { e ⇒
        val initialState = getState(e)
        transitionStates.toList.dropWhile(_ != initialState).drop(1).foldLeft(e) _
      }
      .getOrElse(transitionStates.tail.foldLeft(updateState(transitionStates.head)) _)

    transition { (_, state) ⇒
      val updated = updateState(state)
      getState(updated) must === (state)
      updated
    }
  }

  def eternalActivity(): Map[String, Json] =
    Map("activeFrom" → (("t" → "datetime") ~ ("v" → Instant.ofEpochMilli(1).toString)),
        "activeTo"   → (("t" → "datetime") ~ ("v" → JNull)))

  trait Catalog_ApiFixture {
    private val createPayload = CreateCatalogPayload(name = "default",
                                                     site = Some("stage.foxcommerce.com"),
                                                     countryId = 234,
                                                     defaultLanguage = "en")

    val catalog: CatalogResponse.Root =
      catalogsApi.create(createPayload)(defaultAdminAuth).as[CatalogResponse.Root]
  }

  def randomProductName: String = s"testprod_${Lorem.numerify("####")}"
  def randomSkuCode: String     = s"sku_${Lorem.letterify("????").toUpperCase}"
  def randomSkuPrice: Long      = Random.nextInt(20000).toLong + 100

  case class ProductSku_ApiFixture(productName: String = randomProductName,
                                   skuCode: String = randomSkuCode,
                                   skuPrice: Long = randomSkuPrice) {
    val skuPayload = SkuPayload(
      attributes = Map("code"        → tv(skuCode),
                       "title"       → tv(skuCode.capitalize),
                       "salePrice"   → usdPrice(skuPrice),
                       "retailPrice" → usdPrice(skuPrice)) ++ eternalActivity())

    val productPayload =
      CreateProductPayload(attributes =
                             Map("name" → tv(productName), "title" → tv(productName)) ++ eternalActivity(),
                           skus = Seq(skuPayload),
                           variants = None)

    val product: ProductRoot =
      productsApi.create(productPayload)(implicitly, defaultAdminAuth).as[ProductRoot]

    val sku: SkuResponse.Root = product.skus.onlyElement
  }

  trait ProductVariants_ApiFixture {
    val productCode: String = s"testprod_${Lorem.numerify("####")}"
    val skuCodes: Seq[String] =
      (1 to 2).map(_ ⇒ s"$productCode-sku_${Lorem.letterify("????").toUpperCase}")
    def skuPrice: Long = Random.nextInt(20000).toLong + 100

    private val skuPayloads = skuCodes.map { skuCode ⇒
      SkuPayload(
        attributes = Map("code"        → tv(skuCode),
                         "title"       → tv(skuCode.capitalize),
                         "salePrice"   → usdPrice(skuPrice),
                         "retailPrice" → usdPrice(skuPrice)) ++ eternalActivity())
    }
    private val variantValues = skuCodes.map { skuCode ⇒
      VariantValuePayload(name = s"""productCode-variantValue${Lorem.letterify("???")}""".some,
                          skuCodes = Seq(skuCode),
                          swatch = None,
                          image = None)
    }

    private val variant = VariantPayload(values = Some(variantValues),
                                         attributes = Map("name" → (("t" → "string") ~ ("v" → "Color"))))

    val productPayload =
      CreateProductPayload(
        attributes =
          Map("name" → tv(productCode.capitalize), "title" → tv(productCode.capitalize)) ++ eternalActivity(),
        skus = skuPayloads,
        slug = "simple-product",
        variants = Some(Seq(variant))
      )

    val product: ProductRoot =
      productsApi.create(productPayload)(implicitly, defaultAdminAuth).as[ProductRoot]
  }

  trait Coupon_AnyQualifier_PercentOff extends CouponFixtureBase {
    def percentOff: Int = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
      Promotion.Coupon,
      PromoOfferBuilder.CartPercentOff(percentOff),
      PromoQualifierBuilder.CartAny)

    def promotion =
      promotionsApi.create(promoPayload)(implicitly, defaultAdminAuth).as[PromotionResponse.Root]
  }

  trait Coupon_TotalQualifier_PercentOff extends CouponFixtureBase {
    def qualifiedSubtotal: Long = 5000
    def percentOff: Int         = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
      Promotion.Coupon,
      PromoOfferBuilder.CartPercentOff(percentOff),
      PromoQualifierBuilder.CartTotalAmount(qualifiedSubtotal))

    def promotion =
      promotionsApi.create(promoPayload)(implicitly, defaultAdminAuth).as[PromotionResponse.Root]
  }

  trait Coupon_NumItemsQualifier_PercentOff extends CouponFixtureBase {
    def qualifiedNumItems: Int = 5
    def percentOff: Int        = 10

    private lazy val promoPayload = PromotionPayloadBuilder.build(
      Promotion.Coupon,
      PromoOfferBuilder.CartPercentOff(percentOff),
      PromoQualifierBuilder.CartNumUnits(qualifiedNumItems))

    lazy val promotion =
      promotionsApi.create(promoPayload)(implicitly, defaultAdminAuth).as[PromotionResponse.Root]
  }

  trait CouponFixtureBase {
    def couponActiveFrom: Instant       = Instant.now.minus(1, DAYS)
    def couponActiveTo: Option[Instant] = None
    def couponUsageRules: CouponUsageRules =
      CouponUsageRules(isUnlimitedPerCode = true, isUnlimitedPerCustomer = true)

    def promotion: PromotionResponse.Root

    lazy val coupon = couponsApi
      .create(CreateCoupon(couponAttrs(couponActiveFrom, couponActiveTo, couponUsageRules), promotion.id))(
        implicitly,
        defaultAdminAuth)
      .as[CouponResponse.Root]

    lazy val couponCode =
      couponsApi(coupon.id).codes.generate(Lorem.letterify("?????"))(defaultAdminAuth).as[String]

    protected def couponAttrs(activeFrom: Instant,
                              activeTo: Option[Instant],
                              usageRules: CouponUsageRules): Map[String, Json] = {
      import org.json4s.Extraction.decompose

      val commonAttrs = Map[String, Any](
        "name"           → "Order coupon",
        "storefrontName" → "Order coupon",
        "description"    → "Order coupon description",
        "details"        → "Order coupon details".richText,
        "usageRules"     → decompose(usageRules).asShadowVal(t = "usageRules"),
        "activeFrom"     → activeFrom
      )

      activeTo.fold(commonAttrs)(act ⇒ commonAttrs + ("activeTo" → act)).asShadow
    }

  }

  trait ProductReviewApiFixture extends ProductSku_ApiFixture {
    val reviewAttributes: Json = ("title" → tv("title")) ~ ("body" → tv("body"))
    private val payload =
      CreateProductReviewByCustomerPayload(attributes = reviewAttributes, sku = skuCode, scope = None)
    val productReview =
      productReviewApi.create(payload)(implicitly, defaultAdminAuth).as[ProductReviewResponse]
  }
}
