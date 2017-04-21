import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import cats._
import cats.implicits._
import failures.CouponFailures.{CouponNotFound, CouponWithCodeCannotBeFound}
import failures.NotFoundFailure404
import failures.ObjectFailures._
import failures.PromotionFailures.PromotionIsNotActive
import models.Reasons
import models.objects.{ObjectContext, ObjectUtils}
import models.promotion.Promotion.{Auto, Coupon}
import models.promotion._
import models.rules.QueryStatement
import models.shipping.{ShippingMethod, ShippingMethods}
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.scalactic.TolerantNumerics
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CouponPayloads.CreateCoupon
import payloads.DiscountPayloads.CreateDiscount
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.CartPayloads.CreateCart
import payloads.PaymentPayloads.{CreateManualStoreCredit, StoreCreditPayment}
import payloads.PromotionPayloads._
import payloads.UpdateShippingMethod
import responses.CouponResponses.CouponResponse
import responses.{CustomerResponse, StoreCreditResponse}
import responses.PromotionResponses.PromotionResponse
import responses.cord.{CartResponse, OrderResponse}
import services.objects.ObjectManager
import services.promotion.PromotionManager
import testutils.PayloadHelpers.tv
import testutils._
import testutils.apis._
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}
import testutils.fixtures.api._
import testutils.fixtures.{BakedFixtures, PromotionFixtures}
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._
import utils.seeds.{Factories, ShipmentSeeds}
import utils.time.RichInstant
import java.time.temporal.ChronoUnit.DAYS

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

  "Should apply order percent off promo+coupon to cart" - {

    implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(1.0)

    val DefaultDiscountPercent = 40

    // Yields (CouponResponse.Root, coupon code)
    def setupPromoAndCoupon(extraPromoAttrs: Map[String, Json] = Map.empty)(
        implicit sl: SL,
        sf: SF): (CouponResponse.Root, String) = {
      val promoId = {
        val promotionPayload = {
          val discountPayload = {
            val discountAttrs = {
              val qualifier = JObject(JField("orderAny", JObject(("", JNothing))))
              val offer = JObject(
                  JField("orderPercentOff", JObject(JField("discount", DefaultDiscountPercent))))
              Map("qualifier" → tv(qualifier, "qualifier"), "offer" → tv(offer, "offer"))
            }

            CreateDiscount(attributes = discountAttrs)
          }

          CreatePromotion(applyType = Promotion.Coupon,
                          name = "testyPromo",
                          // "storefrontName" → tv("<p>Testy promo</p>", "richText")
                          activeFrom = Some(Instant.now), // TODO: really? Not `None`? @michalrus
                          activeTo = None,
                          discounts = Seq(discountPayload),
                          attributes = extraPromoAttrs)
        }

        promotionsApi.create(promotionPayload).as[PromotionResponse.Root].id
      }

      val coupon = {
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
        couponsApi.create(couponPayload).as[CouponResponse.Root]
      }
      (coupon, couponsApi(coupon.id).codes.generate("boom").as[String])
    }

    "from admin UI" in new StoreAdmin_Seed with Customer_Seed with ProductAndSkus_Baked {

      private val (_, couponCode) = setupPromoAndCoupon()

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

      cartWithCoupon.totals.adjustments.toDouble must === (
          cartTotal * (DefaultDiscountPercent / 100.0))
      cartWithCoupon.totals.total.toDouble must === (
          cartTotal * (1.0 - (DefaultDiscountPercent / 100.0)))
    }

    "from storefront UI" in new StoreAdmin_Seed with ProductAndSkus_Baked {

      private val (_, couponCode) = setupPromoAndCoupon()

      withRandomCustomerAuth { implicit auth ⇒
        val cartTotal = POST("v1/my/cart/line-items",
                             Seq(UpdateLineItemsPayload("TEST", 1)),
                             auth.jwtCookie.some).asTheResult[CartResponse].totals.total

        val cartWithCoupon =
          POST(s"v1/my/cart/coupon/$couponCode", auth.jwtCookie.some).asTheResult[CartResponse]

        cartWithCoupon.promotion mustBe 'defined
        cartWithCoupon.coupon mustBe 'defined

        cartWithCoupon.totals.adjustments.toDouble must === (
            cartTotal * (DefaultDiscountPercent / 100.0))
        cartWithCoupon.totals.total.toDouble must === (
            cartTotal * (1.0 - (DefaultDiscountPercent / 100.0)))
      }
    }

    "should update coupon discount when cart becomes clean" in new Fixture
    with ProductSku_ApiFixture {
      private val (_, couponCode) = setupPromoAndCoupon()

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

    "but not after archiving the coupon" in new ProductSku_ApiFixture {
      val (coupon, couponCode) = setupPromoAndCoupon()
      val cart                 = api_newGuestCart
      couponsApi(coupon.id).archive
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon
        .add(couponCode)
        .mustFailWith404(CouponWithCodeCannotBeFound(couponCode))
      cartsApi(cart.referenceNumber).get.asTheResult[CartResponse].promotion mustBe 'empty
    }

    "and not after archiving its promotion" in new ProductSku_ApiFixture {
      val (coupon, couponCode) = setupPromoAndCoupon()
      val cart                 = api_newGuestCart
      promotionsApi(coupon.promotion).delete.mustBeOk()
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon.add(couponCode).mustHaveStatus(StatusCodes.NotFound)
      cartsApi(cart.referenceNumber).get.asTheResult[CartResponse].promotion mustBe 'empty
    }

    "and archived promotions ought to be removed from carts" in new ProductSku_ApiFixture {
      val (coupon, couponCode) = setupPromoAndCoupon()
      val cart                 = api_newGuestCart
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon
        .add(couponCode)
        .asTheResult[CartResponse]
        .promotion mustBe 'defined
      promotionsApi(coupon.promotion).delete.mustBeOk()
      cartsApi(cart.referenceNumber).get.asTheResult[CartResponse].promotion mustBe 'empty
    }

    "but not when the promotion is inactive" in new ProductSku_ApiFixture {
      val (coupon, couponCode) =
        setupPromoAndCoupon(Map("activeFrom" → tv(Instant.now.plus(10, DAYS), "datetime")))
      val cart = api_newGuestCart
      cartsApi(cart.referenceNumber).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      cartsApi(cart.referenceNumber).coupon.add(couponCode).mustFailWith400(PromotionIsNotActive)
    }

    "but not if there’s an auto-promo already applied" in new ProductSku_ApiFixture {
      val percentOff = 37

      val autoPromo = promotionsApi
        .create(
            PromotionPayloadBuilder.build(Promotion.Auto,
                                          PromoOfferBuilder.CartPercentOff(percentOff),
                                          PromoQualifierBuilder.CartAny))
        .as[PromotionResponse.Root]

      val (coupon, couponCode) = setupPromoAndCoupon()

      val refNum = api_newGuestCart.referenceNumber

      def percentOff(p: PromotionResponse.Root): Int =
        (p.discounts.head.attributes \ "offer" \ "v" \ "orderPercentOff" \ "discount").extract[Int]

      val woCoupon = cartsApi(refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
        .asTheResult[CartResponse]

      percentOff(woCoupon.promotion.value) must === (percentOff)

      val withCoupon = cartsApi(refNum).coupon.add(couponCode).asTheResult[CartResponse]

      percentOff(withCoupon.promotion.value) must === (DefaultDiscountPercent)
    }
  }

  trait Fixture extends StoreAdmin_Seed with Promotion_Seed with Coupon_Raw

  trait AutoApplyPromotionSeed extends StoreAdmin_Seed with Promotion_Seed {
    override def createPromotionFromPayload(payload: CreatePromotion) =
      super.createPromotionFromPayload(payload.copy(applyType = Auto))
  }
}
