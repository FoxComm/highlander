import java.time.Instant

import cats.implicits._
import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.Reasons
import models.objects.ObjectContext
import models.promotion.Promotion.{Auto, Coupon}
import models.promotion._
import models.rules.QueryStatement
import models.shipping.{ShippingMethod, ShippingMethods}
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.scalactic.TolerantNumerics
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CartPayloads.CreateCart
import payloads.CouponPayloads.CreateCoupon
import payloads.DiscountPayloads.CreateDiscount
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads.{CreateManualStoreCredit, StoreCreditPayment}
import payloads.PromotionPayloads._
import payloads.UpdateShippingMethod
import responses.CouponResponses.CouponResponse
import responses.PromotionResponses.PromotionResponse
import responses.cord.{CartResponse, OrderResponse}
import responses.{CustomerResponse, StoreCreditResponse}
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
import utils.seeds.Factories
import utils.time.RichInstant

class AutoPromotionsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures
    with ApiFixtures
    with ApiFixtureHelpers
    with PromotionFixtures {

  "with many available, the best one is chosen" in new ProductSku_ApiFixture {
    val percentOffs = List.fill(11)(scala.util.Random.nextInt(100))
    val percentOff  = percentOffs.max

    val promos = percentOffs.map { percentOff ⇒
      promotionsApi
        .create(
            PromotionPayloadBuilder.build(Promotion.Auto,
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

  // FIXME: un-ignore this test when you get historical promotions working for OrderResponse… @michalrus
  "keep correct promo versions for Carts & Orders after admin updates" ignore new ProductSku_ApiFixture
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
                  conditions = Some(
                      parse("""{"comparison": "and", "conditions": []}""").extract[QueryStatement])
              ))
          .gimme
          .id)

    // FIXME: use API
    val reason = Reasons.create(Factories.reason(storeAdmin.accountId)).gimme

    def cartPreCheckout(customer: CustomerResponse.Root): CartResponse = {
      val refNum =
        cartsApi.create(CreateCart(customerId = customer.id.some)).as[CartResponse].referenceNumber
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
      customersApi(customer.id).payments.storeCredit
        .create(
            CreateManualStoreCredit(
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

    val orderA2 = ordersApi(orderA.referenceNumber).get().asTheResult[OrderResponse]
    val cartB2  = cartsApi(cartB.referenceNumber).get().asTheResult[CartResponse]

    def percentOff(p: PromotionResponse.Root): Int =
      (p.discounts.head.attributes \ "offer" \ "v" \ "orderPercentOff" \ "discount").extract[Int]

    percentOff(orderA2.promotion.value) must === (percentOffInitial) // FIXME: this line fails @michalrus
    percentOff(cartB2.promotion.value) must === (percentOffUpdated)
  }

  "should be applied retroactively" in new ProductSku_ApiFixture {
    val customer = api_newCustomer()

    val refNum =
      cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

    val cartWithProduct = cartsApi(refNum).lineItems
      .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      .asTheResult[CartResponse]

    cartWithProduct.promotion mustBe 'empty

    val promo = promotionsApi
      .create(
          PromotionPayloadBuilder.build(Promotion.Auto,
                                        PromoOfferBuilder.CartPercentOff(37),
                                        PromoQualifierBuilder.CartAny))
      .as[PromotionResponse.Root]

    cartsApi(refNum).get.asTheResult[CartResponse].promotion mustBe 'defined
  }

}
