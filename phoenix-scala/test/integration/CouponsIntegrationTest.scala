import java.time.Instant.now
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import failures.CartFailures.OrderAlreadyPlaced
import failures.CouponFailures.CouponIsNotActive
import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.cord.{Carts, Orders}
import models.coupon.Coupon
import models.objects.ObjectContext
import org.json4s.JsonAST._
import payloads.CouponPayloads._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.CartPayloads.CreateCart
import responses.CouponResponses.CouponResponse
import responses.cord.CartResponse
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import utils.db._
import utils.time.RichInstant

class CouponsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with ApiFixtureHelpers
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with ApiFixtures
    with BakedFixtures {

  "POST /v1/coupons/:context" - {
    "create coupon" in new StoreAdmin_Seed with Coupon_TotalQualifier_PercentOff {
      coupon
    }

    "create coupon with invalid date should fail" in new StoreAdmin_Seed
    with Coupon_TotalQualifier_PercentOff {
      private val invalidAttrs = Map[String, Any](
          "name"       → "donkey coupon",
          "activeFrom" → ShadowValue("2016-07-19T08:28:21.405+00:00", "datetime")).asShadow

      couponsApi
        .create(CreateCoupon(attributes = invalidAttrs, promotion = promotion.id))
        .mustFailWith400(
            ShadowAttributeInvalidTime("activeFrom", "JString(2016-07-19T08:28:21.405+00:00)"))
    }
  }

  "DELETE /v1/coupons/:context/:id" - {
    "archive existing coupon" in new StoreAdmin_Seed with Coupon_TotalQualifier_PercentOff {
      val archiveResp = couponsApi(coupon.id).archive().as[CouponResponse.Root]

      withClue(archiveResp.archivedAt.value → now) {
        archiveResp.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "404 for not existing coupon" in new StoreAdmin_Seed with Coupon_TotalQualifier_PercentOff {
      couponsApi(666).archive.mustFailWith404(NotFoundFailure404(Coupon, 666))
    }

    "404 when context not found" in new StoreAdmin_Seed with Coupon_TotalQualifier_PercentOff {
      couponsApi(coupon.id)(ObjectContext(name = "donkeyContext", attributes = JNothing))
        .archive()
        .mustFailWith404(ObjectContextNotFound("donkeyContext"))
    }
  }

  "POST /v1/carts/:refNum/coupon/:code" - {
    "attaches coupon successfully" - {
      "when activeFrom is before now" in new CartCouponFixture {
        val response = cartsApi(cartRef).coupon.add(couponCode).asTheResult[CartResponse]
        response.coupon.value.code must === (couponCode)
        response.promotion mustBe defined
      }

      "when activeFrom is before now and activeTo later than now" in new CartCouponFixture {
        override def couponActiveTo = now.plus(1, DAYS).some

        val response = cartsApi(cartRef).coupon.add(couponCode).asTheResult[CartResponse]
        response.coupon.value.code must === (couponCode)
        response.promotion mustBe 'defined
      }

      "when casing of code is different than what's stored in DB" in new CartCouponFixture {
        override def couponActiveTo = now.plus(1, DAYS).some

        val response =
          cartsApi(cartRef).coupon.add(couponCode.toUpperCase).asTheResult[CartResponse]
        response.coupon.value.code must === (couponCode)
        response.promotion mustBe 'defined
      }

      "and excludes gift card cost from the applied discount" - {
        // discount must be 10% off regular line item cost, not regular + gift card cost

        "for cart total qualifier" in new Coupon_TotalQualifier_PercentOff
        with RegularAndGiftCardLineItemFixture {
          override def qualifiedSubtotal: Int = 2000

          cartsApi(cartRef).coupon
            .add(couponCode)
            .asTheResult[CartResponse]
            .totals
            .adjustments must === (300)
        }

        "for items number qualifier" in new Coupon_NumItemsQualifier_PercentOff
        with RegularAndGiftCardLineItemFixture {
          override def qualifiedNumItems: Int = 1

          cartsApi(cartRef).coupon
            .add(couponCode)
            .asTheResult[CartResponse]
            .totals
            .adjustments must === (300)
        }
      }
    }

    "fails to attach coupon" - {
      "when activeFrom is after now" in new CartCouponFixture {
        override def couponActiveFrom = now.plus(1, DAYS)
        override def couponActiveTo   = now.plus(2, DAYS).some

        cartsApi(cartRef).coupon.add(couponCode).mustFailWith400(CouponIsNotActive)
      }

      "when activeTo is before now" in new CartCouponFixture {
        override def couponActiveFrom = now.minus(2, DAYS)
        override def couponActiveTo   = now.minus(1, DAYS).some

        cartsApi(cartRef).coupon.add(couponCode).mustFailWith400(CouponIsNotActive)
      }

      // TODO @anna: This can be removed once /orders vs /carts routes are split
      "when attaching to order" in new CartCouponFixture {
        (for {
          cart  ← * <~ Carts.mustFindByRefNum(cartRef)
          order ← * <~ Orders.createFromCart(cart, subScope = None)
        } yield order).gimme

        POST(s"v1/orders/$cartRef/coupon/$couponCode").mustFailWith400(OrderAlreadyPlaced(cartRef))
      }

      "because purchased gift card is excluded from qualifier judgement" - {

        "for `carts any` qualifier (cart only has gift card line items)" in new Coupon_AnyQualifier_PercentOff {
          val variantId = new ProductVariant_ApiFixture {}.productVariant.id
          val cartRef   = api_newGuestCart().referenceNumber

          cartsApi(cartRef).lineItems
            .add(Seq(UpdateLineItemsPayload(variantId, 2, giftCardLineItemAttributes)))

          val message = "qualifier orderAnyQualifier rejected order with refNum=BR10001, " +
              "reason: Items in cart are not eligible for discount"
          cartsApi(cartRef).coupon.add(couponCode).mustFailWithMessage(message)
        }

        "for `cart total` qualifier" in new Coupon_TotalQualifier_PercentOff
        with RegularAndGiftCardLineItemFixture {
          override def qualifiedSubtotal: Int = 4000

          val message = "qualifier orderTotalAmountQualifier rejected order with refNum=BR10001, " +
              "reason: Order subtotal is less than 4000"
          cartsApi(cartRef).coupon.add(couponCode).mustFailWithMessage(message)
        }

        "for `items number` qualifier" in new Coupon_NumItemsQualifier_PercentOff
        with RegularAndGiftCardLineItemFixture {
          override def qualifiedNumItems: Int = 2

          val message = "qualifier orderNumUnitsQualifier rejected order with refNum=BR10001, " +
              "reason: Order unit count is less than 2"
          cartsApi(cartRef).coupon.add(couponCode).mustFailWithMessage(message)
        }
      }
    }
  }

  trait CartCouponFixture
      extends StoreAdmin_Seed
      with Coupon_TotalQualifier_PercentOff
      with ProductSku_ApiFixture {

    override def skuPrice: Int          = 3100
    override def qualifiedSubtotal: Int = 3000

    val cartRef: String = {
      val cartRef = cartsApi
        .create(CreateCart(customerId = api_newCustomer().id.some))
        .as[CartResponse]
        .referenceNumber

      cartsApi(cartRef).lineItems
        .add(Seq(UpdateLineItemsPayload(sku = skuCode, quantity = 1)))
        .mustBeOk()

      cartRef
    }
  }

  trait RegularAndGiftCardLineItemFixture extends StoreAdmin_Seed {
    val cartRef = api_newGuestCart().referenceNumber

    private val skuCode   = new ProductSku_ApiFixture { override def skuPrice = 3000 }.skuCode
    private val gcSkuCode = new ProductSku_ApiFixture { override def skuPrice = 2000 }.skuCode

    cartsApi(cartRef).lineItems
      .add(Seq(UpdateLineItemsPayload(skuCode, 1),
               UpdateLineItemsPayload(gcSkuCode, 1, giftCardLineItemAttributes)))
      .mustBeOk()
  }
}
