package testutils.fixtures.api

import cats.data.NonEmptyList
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
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
import responses.SkuResponses.SkuResponse
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}
import utils.aliases.Json
import scala.util.Random

trait ApiFixtures extends SuiteMixin with HttpSupport with PhoenixAdminApi with JwtTestAuth {
  self: FoxSuite ⇒

  /** Transitions through list of states.
    *
    * If `initial` is empty it will transition through all of the passed `transitionStates`.
    * If it's defined it will skip any states in `transitionStates` until `initial` is found.
    */
  def transitionEntity[S, E](transitionStates: NonEmptyList[S], initial: Option[E])(
      getState: E ⇒ S)(updateState: S ⇒ E): E = {
    val transition = initial.map { e ⇒
      val initialState = getState(e)
      transitionStates.toList.dropWhile(_ != initialState).drop(1).foldLeft(e) _
    }.getOrElse(transitionStates.tail.foldLeft(updateState(transitionStates.head)) _)

    transition { (_, state) ⇒
      val updated = updateState(state)
      getState(updated) must === (state)
      updated
    }
  }

  trait ProductSku_ApiFixture {
    val productCode: String = s"testprod_${Lorem.numerify("####")}"
    val skuCode: String     = s"$productCode-sku_${Lorem.letterify("????").toUpperCase}"
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
          skus = Seq(skuPayload),
          variants = None)

    val product: ProductRoot =
      productsApi.create(productPayload)(implicitly, defaultAdminAuth).as[ProductRoot]
    val sku: SkuResponse.Root = product.skus.onlyElement
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
    def qualifiedSubtotal: Int = 5000
    def percentOff: Int        = 10

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

    def promotion: PromotionResponse.Root

    lazy val coupon = couponsApi
      .create(CreateCoupon(couponAttrs(couponActiveFrom, couponActiveTo), promotion.id))(
          implicitly,
          defaultAdminAuth)
      .as[CouponResponse.Root]

    lazy val couponCode =
      couponsApi(coupon.id).codes.generate(Lorem.letterify("?????"))(defaultAdminAuth).as[String]

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
