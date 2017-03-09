import java.time.Instant

import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.objects.{ObjectContext, ObjectUtils}
import models.promotion.Promotion.{Auto, Coupon}
import models.promotion._
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.scalactic.TolerantNumerics
import payloads.CouponPayloads.CreateCoupon
import payloads.DiscountPayloads.CreateDiscount
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.CartPayloads.CreateCart
import payloads.PromotionPayloads._
import responses.CouponResponses.CouponResponse
import responses.PromotionResponses.PromotionResponse
import responses.cord.CartResponse
import services.objects.ObjectManager
import services.promotion.PromotionManager
import testutils.PayloadHelpers.tv
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.ApiFixtures
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
    with ApiFixtures
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
      ObjectManager
        .getFullObject(DbResultT.pure(promotion))
        .gimme
        .getAttribute("activeFrom") must !==(JNothing)
    }

    "updating" in new AutoApplyPromotionSeed {

      var fullPromotion = ObjectManager.getFullObject(DbResultT.pure(promotion)).gimme
      fullPromotion.getAttribute("activeFrom") must === (JNothing)

      val attributes: List[(String, JValue)] =
        IlluminateAlgorithm.projectAttributes(fullPromotion.form.attributes,
                                              fullPromotion.shadow.attributes) match {
          case JObject(f) ⇒ f
          case _          ⇒ List()
        }

      PromotionManager
        .update(promotion.formId, UpdatePromotion(Coupon, attributes.toMap, Seq()), ctx.name, None)
        .gimme

      fullPromotion =
        ObjectManager.getFullObject(Promotions.mustFindById400(fullPromotion.model.id)).gimme
      fullPromotion.getAttribute("activeFrom") must !==(JNothing)
    }
  }

  "Auto-applied promotions" - {
    import org.json4s.jackson.JsonMethods.pretty

    "should ⸮" in new Customer_Seed with ProductSku_ApiFixture {
      val percentOff = 0.4

      val promo = promotionsApi
        .create(
            CreatePromotion(
                applyType = Promotion.Auto,
                discounts = List(
                    CreateDiscount(
                        attributes = Map(
                            "qualifier" → tv(Map("orderAny" → Map.empty), t = "qualifier"),
                            "offer" → tv(Map("orderPercentOff" → Map(
                                                 "discount" → math.round(percentOff * 100))),
                                         t = "offer")
                        )
                    )
                ),
                attributes = Map(
                    "name"           → tv("testyPromo"),
                    "storefrontName" → tv("<p>Testy promo</p>", "reechText"),
                    "activeFrom"     → tv(Instant.now, "datetime"),
                    "activeTo"       → tv(JNull, "datetime")
                )
            ))
        .as[PromotionResponse.Root]

      val refNum =
        cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

      val cartWithProduct = cartsApi(refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
        .asTheResult[CartResponse]

      // FIXME: in CordResponsePromotions.fetchPromoDetails
      //cartWithProduct.promotion mustBe 'defined

      cartWithProduct.coupon mustBe 'empty
      import cartWithProduct.totals

      // TODO: really ceiling? Why not round? @michalrus
      // TODO: With round, I’ve once gotten `1377 did not equal 1376 (PromotionsIntegrationTest.scala:154)` @michalrus
      totals.adjustments must === (math.ceil(sku.attributes.salePrice * percentOff).toInt)
      // TODO: if we were using round everywhere, the following would also be round, not floor. Easier… @michalrus
      totals.total must === (math.floor(sku.attributes.salePrice * (1.0 - percentOff)).toInt)
    }
  }

  "Should apply order percent off promo+coupon to cart" - {

    implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(1.0)

    // Yields coupon code
    def setupPromoAndCoupon()(implicit sl: SL, sf: SF): String = {
      val promoId = {
        val promotionPayload = {
          val discountPayload = {
            val discountAttrs = {
              val qualifier = JObject(JField("orderAny", JObject(("", JNothing))))
              val offer     = JObject(JField("orderPercentOff", JObject(JField("discount", 40))))
              Map("qualifier" → tv(qualifier, "qualifier"), "offer" → tv(offer, "offer"))
            }

            CreateDiscount(attributes = discountAttrs)
          }

          val promoAttrs =
            Map("name" → tv("testyPromo"), "storefrontName" → tv("<p>Testy promo</p>", "richText"))

          CreatePromotion(applyType = Promotion.Coupon,
                          discounts = Seq(discountPayload),
                          attributes = promoAttrs)
        }

        promotionsApi.create(promotionPayload).as[PromotionResponse.Root].id
      }

      val couponId = {
        val couponPayload = {
          val usageRules = JObject(JField("isExclusive", false),
                                   JField("isUnlimitedPerCode", false),
                                   JField("usesPerCode", 1),
                                   JField("isUnlimitedPerCustomer", false),
                                   JField("usesPerCustomer", 1))

          val attrs = Map("usageRules" → tv(usageRules, "usageRules"),
                          "name"           → tv("testyCoupon"),
                          "storefrontName" → tv("<p>Testy coupon</p>", "richText"),
                          "activeFrom"     → tv(Instant.now, "datetime"),
                          "activeTo"       → tv(JNull, "datetime"))
          CreateCoupon(promotion = promoId, attributes = attrs)
        }
        couponsApi.create(couponPayload).as[CouponResponse.Root].id
      }
      couponsApi(couponId).codes.generate("boom").as[String]
    }

    "from admin UI" in new StoreAdmin_Seed with Customer_Seed with ProductAndSkus_Baked {

      private val couponCode = setupPromoAndCoupon()

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

      cartWithCoupon.totals.adjustments.toDouble must === (cartTotal * 0.4)
      cartWithCoupon.totals.total.toDouble must === (cartTotal * 0.6)
    }

    "from storefront UI" in new StoreAdmin_Seed with Customer_Seed with ProductAndSkus_Baked {

      private val couponCode = setupPromoAndCoupon()

      private val cartTotal = POST("v1/my/cart/line-items", Seq(UpdateLineItemsPayload("TEST", 1)))
        .asTheResult[CartResponse]
        .totals
        .total

      private val cartWithCoupon = POST(s"v1/my/cart/coupon/$couponCode").asTheResult[CartResponse]

      cartWithCoupon.promotion mustBe 'defined
      cartWithCoupon.coupon mustBe 'defined

      cartWithCoupon.totals.adjustments.toDouble must === (cartTotal * 0.4)
      cartWithCoupon.totals.total.toDouble must === (cartTotal * 0.6)
    }

    "should update coupon discount when cart becomes clean" in new Fixture
    with ProductSku_ApiFixture {
      private val couponCode = setupPromoAndCoupon()

      POST("v1/my/cart/line-items", Seq(UpdateLineItemsPayload(skuCode, 1))).mustBeOk()

      POST(s"v1/my/cart/coupon/$couponCode").mustBeOk()

      private val emptyCartWithCoupon =
        POST(s"v1/my/cart/line-items", Seq(UpdateLineItemsPayload(skuCode, 0)))
          .asTheResult[CartResponse]

      emptyCartWithCoupon.totals.adjustments must === (0)
      emptyCartWithCoupon.totals.total must === (0)
    }
  }

  trait Fixture extends StoreAdmin_Seed with Promotion_Seed with Coupon_Raw

  trait AutoApplyPromotionSeed extends StoreAdmin_Seed with Promotion_Seed {
    override def createPromotionFromPayload(payload: CreatePromotion) =
      super.createPromotionFromPayload(payload.copy(applyType = Auto))
  }
}
