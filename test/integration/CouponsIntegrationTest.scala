import java.time.Instant
import java.time.temporal.ChronoUnit

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.CartFailures.OrderAlreadyPlaced
import failures.CouponFailures.CouponIsNotActive
import failures.NotFoundFailure404
import failures.ObjectFailures.{ObjectContextNotFound, ShadowAttributeInvalidTime}
import models.StoreAdmins
import models.cord.{Carts, Orders}
import models.coupon.Coupon
import models.customer.Customers
import models.objects.{ObjectContext, ObjectContexts}
import models.product.SimpleContext
import models.promotion.{Promotion, Promotions}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import payloads.CouponPayloads._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads._
import responses.CouponResponses.CouponResponse
import responses.TheResponse
import responses.cart.FullCart
import services.coupon.CouponManager
import services.promotion.PromotionManager
import util.{IntegrationTestBase, TestActivityContext}
import utils.db._
import utils.seeds.Seeds.Factories
import utils.db.ExPostgresDriver.api._
import utils.time.RichInstant

class CouponsIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC {

  "POST /v1/coupons/:context" - {
    "create coupon" in new Fixture {
      val response =
        POST(s"v1/coupons/${context.name}",
             CreateCoupon(form = couponForm, shadow = couponShadow, promotion = promotion.id))

      response.status must === (StatusCodes.OK)
    }
    "create coupon with invalid date should fail" in new Fixture {
      val invalidCouponForm = CreateCouponForm(
          attributes = ("name" → "donkey coupon") ~ ("activeFrom" → "2016-07-19T08:28:21.405+00:00")
      )
      val shadow = CreateCouponShadow(
          attributes = ("name" → (("type" → "string") ~ ("ref" → "name")))
              ~ ("activeFrom"  → (("type" → "string") ~ ("ref" → "activeFrom")))
      )
      val response =
        POST(s"v1/coupons/${context.name}",
             CreateCoupon(form = invalidCouponForm, shadow = shadow, promotion = promotion.id))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (ShadowAttributeInvalidTime(
              "activeFrom",
              "JString(2016-07-19T08:28:21.405+00:00)").description)
    }
  }

  "POST /v1/coupons/:context/:id/archive" - {
    "archive existing coupon" in new Fixture {
      val response = POST(s"v1/coupons/${context.name}/${coupon.form.id}/archive")

      response.status must === (StatusCodes.OK)

      val couponResponse = response.as[CouponResponse.Root]
      withClue(couponResponse.archivedAt.value → Instant.now) {
        couponResponse.archivedAt.value.isBeforeNow === true
      }
    }

    "404 for not existing coupon" in new Fixture {
      val response = POST(s"v1/coupons/${context.name}/666/archive")

      response.status must === (StatusCodes.NotFound)
      response.error === (NotFoundFailure404(Coupon, 666).description)
    }

    "404 when context not found" in new Fixture {
      val contextName = "donkeyContext"
      val response    = POST(s"v1/coupons/$contextName/${coupon.form.id}/archive")

      response.status must === (StatusCodes.NotFound)
      response.error must === (ObjectContextNotFound(contextName).description)
    }
  }

  "POST /v1/orders/:refNum/coupon/:code" - {
    "attaches coupon successfully" - {
      "when activeFrom is before now" in new OrderCouponFixture {
        val response = POST(s"v1/orders/${cart.refNum}/coupon/${fromCode}")

        response.status must === (StatusCodes.OK)
        val theCartResponse = response.as[TheResponse[FullCart.Root]].result

        theCartResponse.referenceNumber must === (cart.refNum)
        theCartResponse.coupon must be('defined)
        theCartResponse.coupon.value.code must === (fromCode)
        theCartResponse.promotion must be('defined)
      }

      "when activeFrom is before now and activeTo later than now" in new OrderCouponFixture {
        val response = POST(s"v1/orders/${cart.refNum}/coupon/${fromToCode}")

        response.status must === (StatusCodes.OK)
        val theCartResponse = response.as[TheResponse[FullCart.Root]].result

        theCartResponse.referenceNumber must === (cart.refNum)
        theCartResponse.coupon must be('defined)
        theCartResponse.coupon.value.code must === (fromToCode)
        theCartResponse.promotion must be('defined)
      }
    }

    "fails to attach coupon" - {
      "when activeFrom is after now" in new OrderCouponFixture {
        val response = POST(s"v1/orders/${cart.refNum}/coupon/${willBeActiveCode}")

        response.status must === (StatusCodes.BadRequest)
        response.error must === (CouponIsNotActive.description)
      }

      "when activeTo is before now" in new OrderCouponFixture {
        val response = POST(s"v1/orders/${cart.refNum}/coupon/${wasActiveCode}")

        response.status must === (StatusCodes.BadRequest)
        response.error must === (CouponIsNotActive.description)
      }

      "when attaching to order" in new OrderCouponFixture {
        val response = POST(s"v1/orders/${order.refNum}/coupon/${fromToCode}")

        response.status must === (StatusCodes.BadRequest)
        response.error must === (OrderAlreadyPlaced(order.refNum).description)
      }
    }
  }

  trait Fixture {
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

    val (storeAdmin, context, promotion, coupon) = (for {
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))
      promoRoot ← * <~ PromotionManager.create(promoPayload, context.name)
      promotion ← * <~ Promotions
                   .filter(_.contextId === context.id)
                   .filter(_.formId === promoRoot.form.id)
                   .filter(_.shadowId === promoRoot.shadow.id)
                   .mustFindOneOr(NotFoundFailure404(Promotion, "test"))

      coupon ← * <~ CouponManager.create(couponPayload(promoRoot.form.id), context.name, None)
    } yield (storeAdmin, context, promotion, coupon)).gimme
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
      fromCoupon   ← * <~ CouponManager.create(couponPayload(fromCouponForm), context.name, None)
      fromToCoupon ← * <~ CouponManager.create(couponPayload(fromToCouponForm), context.name, None)
      wasActiveBeforeCoupon ← * <~ CouponManager.create(couponPayload(wasActiveBeforeCouponForm),
                                                        context.name,
                                                        None)
      willBeActiveCoupon ← * <~ CouponManager.create(couponPayload(willBeActiveCouponForm),
                                                     context.name,
                                                     None)
      _ ← * <~ CouponManager.generateCode(fromCoupon.form.id, fromCode, authedStoreAdmin)
      _ ← * <~ CouponManager.generateCode(fromToCoupon.form.id, fromToCode, authedStoreAdmin)
      _ ← * <~ CouponManager.generateCode(wasActiveBeforeCoupon.form.id,
                                          wasActiveCode,
                                          authedStoreAdmin)
      _ ← * <~ CouponManager.generateCode(willBeActiveCoupon.form.id,
                                          willBeActiveCode,
                                          authedStoreAdmin)
      firstCustomer ← * <~ Customers.create(
                         Factories.customer.copy(email = "first@example.org",
                                                 name = Some("first")))
      otherCustomer ← * <~ Customers.create(
                         Factories.customer.copy(email = "second@example.org",
                                                 name = Some("second")))
      cart ← * <~ Carts.create(Factories.cart.copy(customerId = firstCustomer.id))
      cartForOrder ← * <~ Carts.create(
                        Factories.cart.copy(referenceNumber = "ORDER-123456",
                                            customerId = otherCustomer.id))
      order ← * <~ Orders.create(cartForOrder.toOrder())
    } yield (fromCoupon, fromToCoupon, cart, order)).gimme
  }

}
