import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import core.failures.NotFoundFailure404
import objectframework.IlluminateAlgorithm
import objectframework.ObjectFailures.ObjectContextNotFound
import objectframework.models.ObjectContext
import objectframework.services.ObjectManager
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.scalactic.TolerantNumerics
import phoenix.failures.CouponFailures.CouponWithCodeCannotBeFound
import phoenix.failures.PromotionFailures.PromotionIsNotActive
import phoenix.models.promotion.Promotion.{Auto, Coupon}
import phoenix.models.promotion._
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.payloads.CouponPayloads.CreateCoupon
import phoenix.payloads.DiscountPayloads.CreateDiscount
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.PromotionPayloads._
import phoenix.responses.CouponResponses.CouponResponse
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.promotion.PromotionManager
import phoenix.utils.aliases._
import phoenix.utils.time.RichInstant
import testutils.PayloadHelpers.tv
import testutils._
import testutils.apis._
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}
import testutils.fixtures.api._
import testutils.fixtures.{BakedFixtures, PromotionFixtures}
import core.db._
import core.utils.Money._

class PromotionsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures
    with ApiFixtures
    with ApiFixtureHelpers
    with PromotionFixtures {

  "DELETE /v1/promotions/:context/:id" - {
    "archive existing promotion with attached coupons" in new Fixture {
      val promotionResponse = promotionsApi(promotion.formId).delete().as[PromotionResponse]

      withClue(promotionResponse.archivedAt.value → Instant.now) {
        promotionResponse.archivedAt.value.isBeforeNow mustBe true
      }

      val couponRoot = couponsApi(coupon.id).get().as[CouponResponse]
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
                        discounts = Seq(UpdatePromoDiscount(formDiscount.id, attributes = newDiscountAttrs)))

      promotionsApi(promotion.formId).update(disablePromoPayload).as[PromotionResponse]
    }
  }

  "promotion with 'coupon' apply type should be active on" - {

    "creation" in new StoreAdmin_Seed with Promotion_Seed {
      ObjectManager
        .getFullObject(DbResultT.pure(promotion))
        .gimme
        .getAttribute("activeFrom") must !==(JNothing)
    }

    "updating" in new AutoApplyPromotionSeed {

      var fullPromotion = ObjectManager.getFullObject(DbResultT.pure(promotion)).gimme
      fullPromotion.getAttribute("activeFrom") must === (JNothing)

      val attributes: List[(String, JValue)] =
        IlluminateAlgorithm.projectAttributes(fullPromotion.form.attributes, fullPromotion.shadow.attributes) match {
          case JObject(f) ⇒ f
          case _          ⇒ List()
        }

      PromotionManager
        .update(promotion.formId, UpdatePromotion(Coupon, attributes.toMap, Seq()), ctx.name, None)
        .gimme

      fullPromotion = ObjectManager.getFullObject(Promotions.mustFindById400(fullPromotion.model.id)).gimme
      fullPromotion.getAttribute("activeFrom") must !==(JNothing)
    }
  }

  "Should apply order percent off promo+coupon to cart" - {

    implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(1.0)

    val DefaultDiscountPercent = 40

    // Yields (CouponResponse.Root, coupon code)
    def setupPromoAndCoupon(extraPromoAttrs: Map[String, Json] = Map.empty)(implicit sl: SL,
                                                                            sf: SF): CouponResponse = {
      // TODO: try to reuse PromotionPayloadBuilder? @michalrus
      val promoId = {
        val promotionPayload = {
          val discountPayload = {
            val discountAttrs = {
              val qualifier = JObject(JField("orderAny", JObject(("", JNothing))))
              val offer =
                JObject(JField("orderPercentOff", JObject(JField("discount", DefaultDiscountPercent))))
              Map("qualifier" → tv(qualifier, "qualifier"), "offer" → tv(offer, "offer"))
            }

            CreateDiscount(attributes = discountAttrs)
          }

          val promoAttrs =
            Map("name" → tv("testyPromo"), "storefrontName" → tv("<p>Testy promo</p>", "richText"))

          CreatePromotion(applyType = Promotion.Coupon,
                          discounts = Seq(discountPayload),
                          attributes = promoAttrs ++ extraPromoAttrs)
        }

        promotionsApi.create(promotionPayload).as[PromotionResponse].id
      }

      val coupon = {
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
          CreateCoupon(promotion = promoId,
                       attributes = attrs,
                       singleCode = Some(faker.Lorem.letterify("???????")),
                       generateCodes = None)
        }
        couponsApi.create(couponPayload).as[Seq[CouponResponse]].headOption.value
      }
      coupon
    }

    "from admin UI" in new StoreAdmin_Seed with Customer_Seed with ProductAndSkus_Baked {

      private val couponCode = setupPromoAndCoupon().code

      private val cartRefNum =
        cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

      private val cartTotal = cartsApi(cartRefNum).lineItems
        .add(Seq(UpdateLineItemsPayload("TEST", 1)))
        .asTheResult[CartResponse]
        .totals
        .total

      private val cartWithCoupon =
        cartsApi(cartRefNum).coupon.add(couponCode).asTheResult[CartResponse]

      cartWithCoupon.promotion mustBe 'defined
      cartWithCoupon.coupon mustBe 'defined

      cartWithCoupon.totals.adjustments.toDouble must === (cartTotal * (DefaultDiscountPercent / 100.0))
      cartWithCoupon.totals.total.toDouble must === (cartTotal * (1.0 - (DefaultDiscountPercent / 100.0)))
    }

    "from storefront UI" in new StoreAdmin_Seed with ProductAndSkus_Baked {

      private val couponCode = setupPromoAndCoupon().code

      withRandomCustomerAuth { implicit auth ⇒
        val cartTotal = POST("v1/my/cart/line-items",
                             Seq(UpdateLineItemsPayload("TEST", 1)),
                             auth.jwtCookie.some).asTheResult[CartResponse].totals.total

        val cartWithCoupon =
          POST(s"v1/my/cart/coupon/$couponCode", auth.jwtCookie.some).asTheResult[CartResponse]

        cartWithCoupon.promotion mustBe 'defined
        cartWithCoupon.coupon mustBe 'defined

        cartWithCoupon.totals.adjustments.toDouble must === (cartTotal * (DefaultDiscountPercent / 100.0))
        cartWithCoupon.totals.total.toDouble must === (cartTotal * (1.0 - (DefaultDiscountPercent / 100.0)))
      }
    }

    "should update coupon discount when cart becomes clean" in new Fixture {
      val skuCode = ProductSku_ApiFixture().skuCode

      private val couponCode = setupPromoAndCoupon().code

      withRandomCustomerAuth { implicit auth ⇒
        POST("v1/my/cart/line-items", Seq(UpdateLineItemsPayload(skuCode, 1)), auth.jwtCookie.some)
          .mustBeOk()

        POST(s"v1/my/cart/coupon/$couponCode", auth.jwtCookie.some).mustBeOk()

        val emptyCartWithCoupon = POST(s"v1/my/cart/line-items",
                                       Seq(UpdateLineItemsPayload(skuCode, 0)),
                                       auth.jwtCookie.some).asThe[CartResponse]

        emptyCartWithCoupon.warnings mustBe 'nonEmpty withClue "containing information about removed coupons that no longer apply"

        emptyCartWithCoupon.result.totals.total must === (0)
        emptyCartWithCoupon.result.totals.adjustments must === (0)
      }
    }

    "but not after archiving the coupon" in {
      val skuCode = ProductSku_ApiFixture().skuCode

      val coupon = setupPromoAndCoupon()
      val cart   = api_newGuestCart
      couponsApi(coupon.id).archive
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon
        .add(coupon.code)
        .mustFailWith404(CouponWithCodeCannotBeFound(coupon.code))
      cartsApi(cart.referenceNumber).get.asTheResult[CartResponse].promotion mustBe 'empty
    }

    "and not after archiving its promotion" in {
      val coupon = setupPromoAndCoupon()
      val cart   = api_newGuestCart
      promotionsApi(coupon.promotion).delete.mustBeOk()
      val skuCode = ProductSku_ApiFixture().skuCode
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon.add(coupon.code).mustHaveStatus(StatusCodes.NotFound)
      cartsApi(cart.referenceNumber).get.asTheResult[CartResponse].promotion mustBe 'empty
    }

    "and archived promotions ought to be removed from carts" in {
      val coupon  = setupPromoAndCoupon()
      val cart    = api_newGuestCart
      val skuCode = ProductSku_ApiFixture().skuCode
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon
        .add(coupon.code)
        .asTheResult[CartResponse]
        .promotion mustBe 'defined
      promotionsApi(coupon.promotion).delete.mustBeOk()
      cartsApi(cart.referenceNumber).get.asTheResult[CartResponse].promotion mustBe 'empty
    }

    "but not when the promotion is inactive" in {
      val coupon =
        setupPromoAndCoupon(Map("activeFrom" → tv(Instant.now.plus(10, DAYS), "datetime")))
      val cart    = api_newGuestCart
      val skuCode = ProductSku_ApiFixture().skuCode
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon.add(coupon.code).mustFailWith400(PromotionIsNotActive)
    }

    "but not if there’s an auto-promo already applied" in {
      val percentOff = 37

      val autoPromo = promotionsApi
        .create(PromotionPayloadBuilder
          .build(Promotion.Auto, PromoOfferBuilder.CartPercentOff(percentOff), PromoQualifierBuilder.CartAny))
        .as[PromotionResponse]

      val coupon = setupPromoAndCoupon()

      val refNum = api_newGuestCart.referenceNumber

      def getPercentOff(p: PromotionResponse): Int =
        (p.discounts.head.attributes \ "offer" \ "v" \ "orderPercentOff" \ "discount").extract[Int]

      val skuCode = ProductSku_ApiFixture().skuCode
      val woCoupon = cartsApi(refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
        .asTheResult[CartResponse]

      getPercentOff(woCoupon.promotion.value) must === (percentOff)

      val withCoupon = cartsApi(refNum).coupon.add(coupon.code).asTheResult[CartResponse]

      getPercentOff(withCoupon.promotion.value) must === (DefaultDiscountPercent)
    }
  }

  trait Fixture extends StoreAdmin_Seed with Promotion_Seed with Coupon_Raw

  trait AutoApplyPromotionSeed extends StoreAdmin_Seed with Promotion_Seed {
    override def createPromotionFromPayload(payload: CreatePromotion) =
      super.createPromotionFromPayload(payload.copy(applyType = Auto))
  }
}
