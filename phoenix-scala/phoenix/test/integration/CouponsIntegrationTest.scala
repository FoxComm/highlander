import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import core.failures.NotFoundFailure404
import objectframework.ObjectFailures.ObjectContextNotFound
import objectframework.models.ObjectContext
import org.json4s.JsonAST._
import phoenix.failures.CartFailures.OrderAlreadyPlaced
import phoenix.failures.CouponFailures.CouponIsNotActive
import phoenix.models.cord.{Carts, Orders}
import phoenix.models.coupon.Coupon
import phoenix.models.traits.IlluminatedModel
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.responses.CouponResponses.CouponResponse
import phoenix.responses.cord.CartResponse
import phoenix.utils.time.RichInstant
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import core.utils.Money._
import core.db._

class CouponsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with ApiFixtureHelpers
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with ApiFixtures
    with BakedFixtures {

  "POST /v1/coupons/:context" - {
    "create coupon" in new Coupon_TotalQualifier_PercentOff {
      coupon
    }

    "created coupon should always be active" in new Coupon_TotalQualifier_PercentOff {
      override def couponActiveFrom = Instant.now.plus(10, DAYS)
      override def couponActiveTo   = Some(Instant.now.plus(20, DAYS))

      coupon

      val whatAmIDoing = new IlluminatedModel[Unit] {
        def archivedAt    = None
        def attributes    = coupon.attributes
        def inactiveError = CouponIsNotActive
      }

      whatAmIDoing.mustBeActive mustBe 'right
    }
  }

  "DELETE /v1/coupons/:context/:id" - {
    "archive existing coupon" in new Coupon_TotalQualifier_PercentOff {
      val archiveResp = couponsApi(coupon.id).archive().as[CouponResponse.Root]

      withClue(archiveResp.archivedAt.value → now) {
        archiveResp.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "404 for not existing coupon" in new Coupon_TotalQualifier_PercentOff {
      couponsApi(666).archive.mustFailWith404(NotFoundFailure404(Coupon, 666))
    }

    "404 when context not found" in new Coupon_TotalQualifier_PercentOff {
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
          override def qualifiedSubtotal: Long = 2000

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
      // TODO @anna: This can be removed once /orders vs /carts routes are split
      "when attaching to order" in new CartCouponFixture {
        (for {
          cart  ← * <~ Carts.mustFindByRefNum(cartRef)
          order ← * <~ Orders.createFromCart(cart, subScope = None)
        } yield order).gimme

        POST(s"v1/orders/$cartRef/coupon/$couponCode", defaultAdminAuth.jwtCookie.some)
          .mustFailWith400(OrderAlreadyPlaced(cartRef))
      }

      "because purchased gift card is excluded from qualifier judgement" - {

        "for `carts any` qualifier (cart only has gift card line items)" in new Coupon_AnyQualifier_PercentOff {
          val skuCode = ProductSku_ApiFixture().skuCode
          val cartRef = api_newGuestCart().referenceNumber

          cartsApi(cartRef).lineItems
            .add(Seq(UpdateLineItemsPayload(skuCode, 2, randomGiftCardLineItemAttributes)))

          val message = s"qualifier orderAnyQualifier rejected order with refNum=$cartRef, " +
            "reason: Items in cart are not eligible for discount"
          cartsApi(cartRef).coupon.add(couponCode).mustFailWithMessage(message)
        }

        "for `cart total` qualifier" in new Coupon_TotalQualifier_PercentOff
        with RegularAndGiftCardLineItemFixture {
          override def qualifiedSubtotal: Long = 4000

          val message = s"qualifier orderTotalAmountQualifier rejected order with refNum=$cartRef, " +
            s"reason: Order subtotal is less than $qualifiedSubtotal"
          cartsApi(cartRef).coupon.add(couponCode).mustFailWithMessage(message)
        }

        "for `items number` qualifier" in new Coupon_NumItemsQualifier_PercentOff
        with RegularAndGiftCardLineItemFixture {
          override def qualifiedNumItems: Int = 2

          val message = s"qualifier orderNumUnitsQualifier rejected order with refNum=$cartRef, " +
            s"reason: Order unit count is less than $qualifiedNumItems"
          cartsApi(cartRef).coupon.add(couponCode).mustFailWithMessage(message)
        }
      }
    }
  }

  trait CartCouponFixture extends StoreAdmin_Seed with Coupon_TotalQualifier_PercentOff {

    val skuCode = ProductSku_ApiFixture(skuPrice = 3100).skuCode

    override def qualifiedSubtotal: Long = 3000

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

    private val skuCode   = ProductSku_ApiFixture(skuPrice = 3000).skuCode
    private val gcSkuCode = ProductSku_ApiFixture(skuPrice = 2000).skuCode

    cartsApi(cartRef).lineItems
      .add(Seq(UpdateLineItemsPayload(skuCode, 1),
               UpdateLineItemsPayload(gcSkuCode, 1, randomGiftCardLineItemAttributes())))
      .mustBeOk()
  }
}
