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
import org.json4s.JsonAST.JNothing
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
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time.RichInstant

class CouponsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/coupons/:context" - {
    "create coupon" in new Fixture {
      couponsApi
        .create(CreateCoupon(form = couponForm, shadow = couponShadow, promotion = promotion.id))
        .mustBeOk()
    }

    "create coupon with invalid date should fail" in new Fixture {
      val invalidCouponForm = CreateCouponForm(
          attributes = ("name" → "donkey coupon") ~ ("activeFrom" → "2016-07-19T08:28:21.405+00:00")
      )
      val shadow = CreateCouponShadow(
          attributes = ("name" → (("type" → "string") ~ ("ref" → "name")))
              ~ ("activeFrom"  → (("type" → "string") ~ ("ref" → "activeFrom")))
      )
      couponsApi
        .create(CreateCoupon(form = invalidCouponForm, shadow = shadow, promotion = promotion.id))
        .mustFailWith400(
            ShadowAttributeInvalidTime("activeFrom", "JString(2016-07-19T08:28:21.405+00:00)"))
    }
  }

  "DELETE /v1/coupons/:context/:id" - {
    "archive existing coupon" in new Fixture {
      val couponResponse = couponsApi.delete(coupon.form.id).as[CouponResponse.Root]
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
        .delete(coupon.form.id)(donkeyContext)
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

    implicit val au = storeAdminAuthData

    val percentOff   = 10
    val totalAmount  = 0
    val discountForm = CreateDiscountForm(attributes = parse(s"""
    {
      "title" : "Get $percentOff% off when you spend $totalAmount dollars",
      "description" : "$percentOff% off when you spend over $totalAmount dollars",
      "tags" : [],
      "qualifier" : {
        "orderTotalAmount" : {
          "totalAmount" : ${totalAmount * 100}
        }
      },
      "offer" : {
        "orderPercentOff": {
          "discount": $percentOff
        }
      }
    }"""))
    val discountShadow = CreateDiscountShadow(
        attributes = parse("""
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "tags" : {"type": "tags", "ref": "tags"},
          "qualifier" : {"type": "qualifier", "ref": "qualifier"},
          "offer" : {"type": "offer", "ref": "offer"}
        }"""))

    val promoForm = CreatePromotionForm(attributes = ("name"
                                                → (("t" → "string") ~ ("v" → "donkey promo"))),
                                        discounts = Seq(discountForm))
    val promoShadow = CreatePromotionShadow(attributes = ("name"
                                                    → (("type" → "string") ~ ("ref" → "name"))),
                                            discounts = Seq(discountShadow))
    val promoPayload = CreatePromotion(applyType = Promotion.Coupon, promoForm, promoShadow)

    val couponForm = CreateCouponForm(attributes = ("name" → "donkey coupon"))

    val couponShadow = CreateCouponShadow(
        attributes = ("name"
                → (("type" → "string") ~ ("ref" → "name"))))

    def couponPayload(promoId: Int): CreateCoupon = CreateCoupon(couponForm, couponShadow, promoId)

    val (promotion, coupon) = (for {
      promoRoot ← * <~ PromotionManager.create(promoPayload, ctx.name)
      promotion ← * <~ Promotions
                   .filter(_.contextId === ctx.id)
                   .filter(_.formId === promoRoot.form.id)
                   .filter(_.shadowId === promoRoot.shadow.id)
                   .mustFindOneOr(NotFoundFailure404(Promotion, "test"))

      coupon ← * <~ CouponManager.create(couponPayload(promoRoot.form.id), ctx.name, None)
    } yield (promotion, coupon)).gimme
  }

  trait OrderCouponFixture extends Fixture {

    val fromCouponForm = CreateCouponForm(
        attributes = (("name"     → "Order coupon") ~
                ("storefrontName" → "Order coupon") ~
                ("description"    → "Order coupon description") ~
                ("details"        → "Order coupon details") ~
                ("usageRules"                       → (("isExclusive" → true) ~ ("isUnlimitedPerCode" → true) ~
                          ("isUnlimitedPerCustomer" → true))) ~
                ("activeFrom"                       → Instant.now.minus(1, ChronoUnit.DAYS).toString)))

    val fromToCouponForm = CreateCouponForm(
        attributes = (("name" → "Order coupon")) ~
            ("storefrontName" → "Order coupon") ~
            ("description"    → "Order coupon description") ~
            ("details"        → "Order coupon details") ~
            ("usageRules"     → (("isExclusive" → true) ~ ("isUnlimitedPerCode" → true) ~ ("isUnlimitedPerCustomer" → true))) ~
            ("activeFrom"     → Instant.now.minus(1, ChronoUnit.DAYS).toString) ~
            ("activeTo"       → Instant.now.plus(1, ChronoUnit.DAYS).toString))

    val wasActiveBeforeCouponForm = CreateCouponForm(
        attributes = (("name" → "Order coupon")) ~
            ("storefrontName" → "Order coupon") ~
            ("description"    → "Order coupon description") ~
            ("details"        → "Order coupon details") ~
            ("usageRules"     → (("isExclusive" → true) ~ ("isUnlimitedPerCode" → true) ~ ("isUnlimitedPerCustomer" → true))) ~
            ("activeFrom"     → Instant.now.minus(2, ChronoUnit.DAYS).toString) ~
            ("activeTo"       → Instant.now.minus(1, ChronoUnit.DAYS).toString))

    val willBeActiveCouponForm = CreateCouponForm(
        attributes = (("name" → "Order coupon")) ~
            ("storefrontName" → "Order coupon") ~
            ("description"    → "Order coupon description") ~
            ("details"        → "Order coupon details") ~
            ("usageRules"     → (("isExclusive" → true) ~ ("isUnlimitedPerCode" → true) ~ ("isUnlimitedPerCustomer" → true))) ~
            ("activeFrom"     → Instant.now.plus(1, ChronoUnit.DAYS).toString) ~
            ("activeTo"       → Instant.now.plus(2, ChronoUnit.DAYS).toString))

    val orderCouponShadow = CreateCouponShadow(
        ("name"             → (("type" → "string") ~ ("ref"     → "name"))) ~
          ("storefrontName" → (("type" → "richText") ~ ("ref"   → "storefrontName"))) ~
          ("description"    → (("type" → "text") ~ ("ref"       → "description"))) ~
          ("details"        → (("type" → "richText") ~ ("ref"   → "details"))) ~
          ("usageRules"     → (("type" → "usageRules") ~ ("ref" → "usageRules"))) ~
          ("activeFrom"     → (("type" → "dateTime") ~ ("ref"   → "activeFrom")) ~
                ("activeTo" → (("type" → "dateTime") ~ ("ref"   → "activeTo")))))

    val fromCode         = "activeWithFrom"
    val fromToCode       = "activeWithFromTo"
    val wasActiveCode    = "wasActiveCode"
    val willBeActiveCode = "willBeActiveCode"

    def couponPayload(form: CreateCouponForm): CreateCoupon =
      CreateCoupon(form, orderCouponShadow, promotion.formId)

    val (fromCoupon, fromToCoupon, cart, order) = (for {
      fromCoupon   ← * <~ CouponManager.create(couponPayload(fromCouponForm), ctx.name, None)
      fromToCoupon ← * <~ CouponManager.create(couponPayload(fromToCouponForm), ctx.name, None)
      wasActiveBeforeCoupon ← * <~ CouponManager.create(couponPayload(wasActiveBeforeCouponForm),
                                                        ctx.name,
                                                        None)
      willBeActiveCoupon ← * <~ CouponManager.create(couponPayload(willBeActiveCouponForm),
                                                     ctx.name,
                                                     None)
      _            ← * <~ CouponManager.generateCode(fromCoupon.form.id, fromCode, authedUser)
      _            ← * <~ CouponManager.generateCode(fromToCoupon.form.id, fromToCode, authedUser)
      _            ← * <~ CouponManager.generateCode(wasActiveBeforeCoupon.form.id, wasActiveCode, authedUser)
      _            ← * <~ CouponManager.generateCode(willBeActiveCoupon.form.id, willBeActiveCode, authedUser)
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
      cart ← * <~ Carts.create(
                Factories.cart(Scope.current).copy(accountId = firstCustomer.accountId))
      cartForOrder ← * <~ Carts.create(
                        Factories
                          .cart(Scope.current)
                          .copy(referenceNumber = "ORDER-123456",
                                accountId = otherCustomer.accountId))
      order ← * <~ Orders.createFromCart(cartForOrder)
    } yield (fromCoupon, fromToCoupon, cart, order)).gimme
  }

}
