import java.time.Instant
import java.time.temporal.ChronoUnit

import com.sksamuel.elastic4s.mappings
import com.sksamuel.elastic4s.mappings.attributes
import failures.CartFailures.OrderAlreadyPlaced
import failures.CouponFailures.CouponIsNotActive
import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.account._
import models.cord.{Carts, Orders}
import models.coupon.Coupon
import models.customer._
import models.objects.ObjectContext
import models.promotion.{Promotion, Promotions}
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import payloads.CouponPayloads._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads._
import responses.CouponResponses.CouponResponse
import responses.cord.CartResponse
import services.coupon.CouponManager
import services.promotion.PromotionManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.PayloadHelpers._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time.RichInstant
import utils.aliases.Json

class CouponsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/coupons/:context" - {
    "create coupon" in new Fixture {
      couponsApi
        .create(CreateCoupon(attributes = couponAttributes, promotion = promotion.id))
        .mustBeOk()
    }

    "create coupon with invalid date should fail" in new Fixture {
      val invalidCouponAttributes = Map[String, Any](
          "name"       → "donkey coupon",
          "activeFrom" → ShadowValue("2016-07-19T08:28:21.405+00:00", "datetime")).asShadow

      couponsApi
        .create(CreateCoupon(attributes = invalidCouponAttributes, promotion = promotion.id))
        .mustFailWith400(
            ShadowAttributeInvalidTime("activeFrom", "JString(2016-07-19T08:28:21.405+00:00)"))
    }
  }

  "DELETE /v1/coupons/:context/:id" - {
    "archive existing coupon" in new Fixture {
      val couponResponse = couponsApi.delete(coupon.id).as[CouponResponse.Root]
      withClue(couponResponse.archivedAt.value → Instant.now) {
        couponResponse.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "404 for not existing coupon" in new Fixture {
      couponsApi.delete(666).mustFailWith404(NotFoundFailure404(Coupon, 666))
    }

    "404 when context not found" in new Fixture {
      implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)
      couponsApi
        .delete(coupon.id)(donkeyContext)
        .mustFailWith404(ObjectContextNotFound("donkeyContext"))
    }
  }

  "POST /v1/orders/:refNum/coupon/:code" - {
    "attaches coupon successfully" - {
      "when activeFrom is before now" in new OrderCouponFixture {
        val response = cartsApi(cart.refNum).coupon.add(fromCode).asTheResult[CartResponse]

        response.referenceNumber must === (cart.refNum)
        response.coupon.value.code must === (fromCode)
        response.promotion mustBe defined
      }

      "when activeFrom is before now and activeTo later than now" in new OrderCouponFixture {
        val response = cartsApi(cart.refNum).coupon.add(fromToCode).asTheResult[CartResponse]

        response.referenceNumber must === (cart.refNum)
        response.coupon.value.code must === (fromToCode)
        response.promotion mustBe 'defined
      }
    }

    "fails to attach coupon" - {
      "when activeFrom is after now" in new OrderCouponFixture {
        cartsApi(cart.refNum).coupon.add(willBeActiveCode).mustFailWith400(CouponIsNotActive)
      }

      "when activeTo is before now" in new OrderCouponFixture {
        cartsApi(cart.refNum).coupon.add(wasActiveCode).mustFailWith400(CouponIsNotActive)
      }

      "when attaching to order" in new OrderCouponFixture {
        // TODO @anna: This can be removed once /orders vs /carts pathes are split
        POST(s"v1/orders/${order.refNum}/coupon/$fromToCode")
          .mustFailWith400(OrderAlreadyPlaced(order.refNum))
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val percentOff  = 10
    val totalAmount = 0
    val discountAttributes = Map[String, Json](
        "title"       → tv(s"Get $percentOff% off when you spend $totalAmount dollars"),
        "description" → tv(s"$percentOff% off when you spend over $totalAmount dollars"),
        "tags"        → tv(JArray(List.empty[Json]), "tags"),
        "qualifier" → (("t" → "qualifier") ~ ("v" →
                  JObject("orderTotalAmount" → JObject(
                          "totalAmount" → JInt(totalAmount * 100)
                      )))),
        "offer" → (("t" → "offer") ~ ("v" →
                  JObject("orderPercentOff" → JObject(
                          "discount" → JInt(percentOff)
                      ))))
    )

    val promoAttributes = Map[String, Json]("name" → tv("donkey promo"))

    val promoPayload = CreatePromotion(applyType = Promotion.Coupon,
                                       attributes = promoAttributes,
                                       discounts =
                                         Seq(CreateDiscount(attributes = discountAttributes)))

    val couponAttributes = Map[String, Json]("name" → tv("donkey promo"))

    def couponPayload(promoId: Int): CreateCoupon = CreateCoupon(couponAttributes, promoId)

    val (promotion, coupon) = (for {
      promoRoot ← * <~ PromotionManager.create(promoPayload, ctx.name)
      promotion ← * <~ Promotions
                   .filter(_.contextId === ctx.id)
                   .filter(_.formId === promoRoot.id)
                   .mustFindOneOr(NotFoundFailure404(Promotion, "test"))

      coupon ← * <~ CouponManager.create(couponPayload(promoRoot.id), ctx.name, None)
    } yield (promotion, coupon)).gimme
  }

  trait OrderCouponFixture extends Fixture {

    val fromCouponAttributes: Map[String, Json] = Map[String, Any](
        "name"           → "Order coupon",
        "storefrontName" → "Order coupon",
        "description"    → "Order coupon description",
        "details"        → "Order coupon details",
        "usageRules" → (("isExclusive" → true) ~
              ("isUnlimitedPerCode"     → true) ~
              ("isUnlimitedPerCustomer" → true)).asShadowVal(t = "usageRules"),
        "activeFrom"                    → Instant.now.minus(1, ChronoUnit.DAYS)).asShadow

    val fromToCouponAttributes: Map[String, Json] = Map[String, Any](
        "name"           → "Order coupon",
        "storefrontName" → "Order coupon",
        "description"    → "Order coupon description",
        "details"        → "Order coupon details".richText,
        "usageRules" → (("isExclusive" → true) ~
              ("isUnlimitedPerCode"     → true) ~
              ("isUnlimitedPerCustomer" → true)).asShadowVal(t = "usageRules"),
        "activeFrom"                    → Instant.now.minus(1, ChronoUnit.DAYS),
        "activeTo"                      → Instant.now.plus(1, ChronoUnit.DAYS)).asShadow

    val wasActiveBeforeCouponAttributes: Map[String, Json] = Map[String, Any](
        "name"           → "Order coupon",
        "storefrontName" → "Order coupon",
        "description"    → "Order coupon description",
        "details"        → "Order coupon details".richText,
        "usageRules"                    → (("isExclusive" → true) ~ ("isUnlimitedPerCode" → true) ~
              ("isUnlimitedPerCustomer" → true)).asShadowVal(t = "usageRules"),
        "activeFrom"                    → Instant.now.minus(2, ChronoUnit.DAYS),
        "activeTo"                      → Instant.now.minus(1, ChronoUnit.DAYS)).asShadow

    val willBeActiveCouponAttributes: Map[String, Json] = Map[String, Any](
        "name"           → "Order coupon",
        "storefrontName" → "Order coupon",
        "description"    → "Order coupon description",
        "details"        → "Order coupon details".richText,
        "usageRules" → (("isExclusive" → true) ~
              ("isUnlimitedPerCode" → true) ~ ("isUnlimitedPerCustomer" → true))
          .asShadowVal(t = "usageRules"),
        "activeFrom" → Instant.now.plus(1, ChronoUnit.DAYS),
        "activeTo"   → Instant.now.plus(2, ChronoUnit.DAYS)).asShadow

    val fromCode         = "activeWithFrom"
    val fromToCode       = "activeWithFromTo"
    val wasActiveCode    = "wasActiveCode"
    val willBeActiveCode = "willBeActiveCode"

    def couponPayload(couponAttributes: Map[String, Json]): CreateCoupon =
      CreateCoupon(attributes = couponAttributes, promotion = promotion.formId)

    val (fromCoupon, fromToCoupon, cart, order) = (for {
      fromCoupon ← * <~ CouponManager.create(couponPayload(fromCouponAttributes), ctx.name, None)
      fromToCoupon ← * <~ CouponManager.create(couponPayload(fromToCouponAttributes),
                                               ctx.name,
                                               None)
      wasActiveBeforeCoupon ← * <~ CouponManager.create(
                                 couponPayload(wasActiveBeforeCouponAttributes),
                                 ctx.name,
                                 None)
      willBeActiveCoupon ← * <~ CouponManager.create(couponPayload(willBeActiveCouponAttributes),
                                                     ctx.name,
                                                     None)
      _            ← * <~ CouponManager.generateCode(fromCoupon.id, fromCode, authedUser)
      _            ← * <~ CouponManager.generateCode(fromToCoupon.id, fromToCode, authedUser)
      _            ← * <~ CouponManager.generateCode(wasActiveBeforeCoupon.id, wasActiveCode, authedUser)
      _            ← * <~ CouponManager.generateCode(willBeActiveCoupon.id, willBeActiveCode, authedUser)
      firstAccount ← * <~ Accounts.create(Account())
      firstCustomer ← * <~ Users.create(
                         Factories.customer.copy(accountId = firstAccount.id,
                                                 email = Some("first@example.org"),
                                                 name = Some("first")))
      _ ← * <~ CustomersData.create(
             CustomerData(userId = firstCustomer.id, accountId = firstAccount.id))
      otherAccount ← * <~ Accounts.create(Account())
      otherCustomer ← * <~ Users.create(
                         Factories.customer.copy(accountId = otherAccount.id,
                                                 email = Some("second@example.org"),
                                                 name = Some("second")))
      _ ← * <~ CustomersData.create(
             CustomerData(userId = otherCustomer.id, accountId = otherAccount.id))
      cart ← * <~ Carts.create(Factories.cart.copy(accountId = firstCustomer.accountId))
      cartForOrder ← * <~ Carts.create(
                        Factories.cart.copy(referenceNumber = "ORDER-123456",
                                            accountId = otherCustomer.accountId))
      order ← * <~ Orders.createFromCart(cartForOrder)
    } yield (fromCoupon, fromToCoupon, cart, order)).gimme
  }

}
