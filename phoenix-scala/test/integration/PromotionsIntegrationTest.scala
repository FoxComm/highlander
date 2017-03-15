import java.time.Instant

import cats._
import cats.implicits._
import failures.NotFoundFailure404
import failures.ObjectFailures._
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
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}
import testutils.fixtures.api._
import testutils.fixtures.{BakedFixtures, PromotionFixtures}
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._
import utils.seeds.{Factories, ShipmentSeeds}
import utils.time.RichInstant

class PromotionsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
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

  "Auto-applied promotions:" - {
    "with many available, the best one is chosen" in new ProductSku_ApiFixture {
      val percentOffs = List.fill(11)(scala.util.Random.nextInt(100))
      val percentOff  = percentOffs.max

      val promos = percentOffs.map { percentOff ⇒
        promotionsApi
          .create(PromotionPayloadBuilder.build(Promotion.Auto,
                                                PromoOfferBuilder.CartPercentOff(percentOff),
                                                PromoQualifierBuilder.CartAny))
          .as[PromotionResponse.Root]
      }

      val customer = api_newCustomer()

      val refNum =
        cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

      val cartWithProduct = cartsApi(refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
        .asTheResult[CartResponse]

      cartWithProduct.promotion mustBe 'defined
      cartWithProduct.coupon mustBe 'empty

      import cartWithProduct.totals
      implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(1.0)
      totals.adjustments.toDouble must === (sku.attributes.salePrice * (percentOff / 100.0))
      totals.total.toDouble must === (sku.attributes.salePrice * (1.0 - percentOff / 100.0))
    }

    "keep correct promo versions for Carts & Orders after admin updates" in new ProductSku_ApiFixture
    with StoreAdmin_Seed {
      val percentOffInitial = 33
      val percentOffUpdated = 17

      val promo = promotionsApi
        .create(
            PromotionPayloadBuilder.build(Promotion.Auto,
                                          PromoOfferBuilder.CartPercentOff(percentOffInitial),
                                          PromoQualifierBuilder.CartAny))
        .as[PromotionResponse.Root]

      val customerA, customerB = api_newCustomer()

      // FIXME: use API
      val shippingMethod = UpdateShippingMethod(
          ShippingMethods
            .create(
                ShippingMethod(
                    adminDisplayName = "a",
                    storefrontDisplayName = "b",
                    code = "c",
                    price = 1000,
                    conditions = Some(parse("""{"comparison": "and", "conditions": []}""")
                          .extract[QueryStatement])
                ))
            .gimme
            .id)

      // FIXME: use API
      val reason = Reasons.create(Factories.reason(storeAdmin.accountId)).gimme

      def cartPreCheckout(customer: CustomerResponse.Root): CartResponse = {
        val refNum = cartsApi
          .create(CreateCart(customerId = customer.id.some))
          .as[CartResponse]
          .referenceNumber
        cartsApi(refNum).lineItems
          .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
          .asTheResult[CartResponse]
        cartsApi(refNum).shippingAddress
          .create(
              CreateAddressPayload(name = "Home Office",
                                   regionId = 1,
                                   address1 = "3000 Coolio Dr",
                                   city = "Seattle",
                                   zip = "55555"))
          .asTheResult[CartResponse]
        val c = cartsApi(refNum).shippingMethod.update(shippingMethod).asTheResult[CartResponse]
        import c.totals.total
        val scr = customersApi(customer.id).payments.storeCredit
          .create(CreateManualStoreCredit(
                  amount = total,
                  reasonId = reason.id
              ))
          .as[StoreCreditResponse.Root]
        cartsApi(refNum).payments.storeCredit
          .add(StoreCreditPayment(total))
          .asTheResult[CartResponse]
      }

      val List(cartA, cartB) = List(customerA, customerB).map(cartPreCheckout)

      val orderA = cartsApi(cartA.referenceNumber).checkout().as[OrderResponse]

      // Now, let’s update the promotion and see if orderA’s one stays intact, while cartB’s is updated.

      val promoUpdated = promotionsApi(promo.id).update {
        val payload =
          PromotionPayloadBuilder.build(Promotion.Auto,
                                        PromoOfferBuilder.CartPercentOff(percentOffUpdated),
                                        PromoQualifierBuilder.CartAny)
        UpdatePromotion(
            applyType = payload.applyType,
            attributes = payload.attributes,
            discounts =
              Seq(UpdatePromoDiscount(promo.discounts.head.id, payload.discounts.head.attributes)))
      }.as[PromotionResponse.Root]

      println("----------------------------- FETCHING ORDER ----------------------------------")
      val orderA2 = ordersApi(orderA.referenceNumber).get().asTheResult[OrderResponse]

      info(s"upd = $orderA2")

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
        POST(s"v1/my/cart/line-items", Seq(UpdateLineItemsPayload(skuCode, 0))).asThe[CartResponse]

      emptyCartWithCoupon.warnings mustBe 'nonEmpty withClue "containing information about removed coupons that no longer apply"

      emptyCartWithCoupon.result.totals.total must === (0)
      emptyCartWithCoupon.result.totals.adjustments must === (0)
    }
  }

  trait Fixture extends StoreAdmin_Seed with Promotion_Seed with Coupon_Raw

  trait AutoApplyPromotionSeed extends StoreAdmin_Seed with Promotion_Seed {
    override def createPromotionFromPayload(payload: CreatePromotion) =
      super.createPromotionFromPayload(payload.copy(applyType = Auto))
  }
}
