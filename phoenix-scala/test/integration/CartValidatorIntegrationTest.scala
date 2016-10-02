import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import util.Extensions._
import cats.implicits._
import failures.CartFailures._
import failures.Failure
import models.cord._
import models.inventory.Skus
import models.location.Addresses
import models.objects.ObjectContexts
import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.payment.storecredit._
import models.product.{Mvp, SimpleContext}
import models.shipping.ShippingMethods
import org.scalatest.AppendedClues
import payloads.AddressPayloads._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import responses.TheResponse
import responses.cord.CartResponse
import util._
import util.fixtures.BakedFixtures
import utils.aliases._
import utils.db._
import utils.seeds.CouponSeeds
import utils.seeds.Seeds.Factories

class CartValidatorIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AppendedClues
    with AutomaticAuth
    with BakedFixtures {

  "Cart validator must be applied to" - {

    "/v1/orders/:refNum/payment-methods/gift-cards" in new GiftCardFixture {
      val api = cartsApi(refNum).payments.giftCard
      checkResponse(api.add(GiftCardPayment(giftCard.code)), expectedWarnings)
      checkResponse(api.delete(giftCard.code), expectedWarnings)
    }

    "/v1/orders/:refNum/payment-methods/store-credit" in new StoreCreditFixture {
      val api = cartsApi(refNum).payments.storeCredit
      checkResponse(api.add(StoreCreditPayment(500)), expectedWarnings)
      checkResponse(api.delete(), expectedWarnings)
    }

    "/v1/orders/:refNum/payment-methods/credit-cards" in new CreditCardFixture {
      val api = cartsApi(refNum).payments.creditCard
      checkResponse(api.add(CreditCardPayment(creditCard.id)), expectedWarnings)
      checkResponse(api.delete(), expectedWarnings)
    }

    "/v1/orders/:refNum/shipping-address" in new ShippingAddressFixture {
      val api = cartsApi(refNum).shippingAddress

      checkResponse(api.create(CreateAddressPayload("a", 1, "b", None, "c", "11111")),
                    expectedWarnings)

      checkResponse(api.update(UpdateAddressPayload(name = "z".some)), expectedWarnings)

      checkResponse(api.delete(), expectedWarnings :+ NoShipAddress(refNum))

      val address = Addresses.create(Factories.address.copy(customerId = customer.id)).gimme
      checkResponse(api.updateFromAddress(address.id), expectedWarnings)
    }

    "/v1/orders/:refNum/shipping-method" in new ShippingMethodFixture {
      val api = cartsApi(refNum).shippingMethod
      checkResponse(api.update(UpdateShippingMethod(shipMethod.id)),
                    Seq(EmptyCart(refNum), InsufficientFunds(refNum)))
      checkResponse(api.delete(), Seq(EmptyCart(refNum), NoShipMethod(refNum)))
    }

    "/v1/orders/:refNum/line-items" in new LineItemFixture {
      checkResponse(cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))),
                    Seq(InsufficientFunds(refNum), NoShipAddress(refNum), NoShipMethod(refNum)))
    }

    "/v1/orders/:refNum/coupon" in new CouponFixture {
      checkResponse(cartsApi(refNum).coupon.add(couponCode), expectedWarnings)
      checkResponse(cartsApi(refNum).coupon.delete(), expectedWarnings)
    }

    "must validate funds with line items:" - {
      "must return warning when credit card is removed" in new LineItemAndFundsFixture {
        val api = cartsApi(refNum)
        api.lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))).mustBeOk()
        api.payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()

        checkResponse(api.payments.creditCard.delete(),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }

      "must return warning when store credits are removed" in new LineItemAndFundsFixture {
        cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))).mustBeOk()
        cartsApi(refNum).payments.storeCredit.add(StoreCreditPayment(500)).mustBeOk()

        checkResponse(cartsApi(refNum).payments.storeCredit.delete(),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }

      "must return warning when gift card is removed" in new LineItemAndFundsFixture {
        cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))).mustBeOk()
        cartsApi(refNum).payments.giftCard.add(GiftCardPayment(giftCard.code)).mustBeOk()

        checkResponse(cartsApi(refNum).payments.giftCard.delete(giftCard.code),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }
    }

    def checkResponse(response: HttpResponse, expectedWarnings: Seq[Failure])(implicit line: SL,
                                                                              file: SF): Unit = {
      val warnings = response.withResultTypeOf[CartResponse].warnings
      warnings.value must not be empty
      warnings.value must contain theSameElementsAs expectedWarnings.map(_.description)
    }
  }

  trait CouponFixture
      extends CouponSeeds
      with TestActivityContext.AdminAC
      with ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with StoreAdmin_Seed {
    val (refNum, couponCode) = (for {
      search     ← * <~ Factories.createSharedSearches(storeAdmin.id)
      discounts  ← * <~ Factories.createDiscounts(search)
      promotions ← * <~ Factories.createCouponPromotions(discounts)
      coupons    ← * <~ Factories.createCoupons(promotions)
      couponCode = coupons.head._2.head.code
    } yield (cart.refNum, couponCode)).gimme
  }

  trait LineItemFixture extends EmptyCustomerCart_Baked {
    val (sku) = (for {
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
    } yield sku).gimme
    val refNum = cart.refNum
  }

  trait ShippingMethodFixture extends EmptyCustomerCart_Baked {
    val (shipMethod) = (for {
      address ← * <~ Addresses.create(
                   Factories.address.copy(customerId = customer.id, regionId = 4129))
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                cordRef = cart.refNum)
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
    } yield shipMethod).gimme
    val refNum = cart.refNum
  }

  trait ShippingAddressFixture extends EmptyCustomerCart_Baked {
    val refNum           = cart.refNum
    val expectedWarnings = Seq(EmptyCart(refNum), NoShipMethod(refNum))
  }

  trait GiftCardFixture
      extends ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with Reason_Baked {
    val giftCard = (for {
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
    val refNum = cart.refNum
  }

  trait StoreCreditFixture
      extends ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with Reason_Baked {
    (for {
      manual ← * <~ StoreCreditManuals.create(
                  StoreCreditManual(adminId = storeAdmin.id, reasonId = reason.id))
      _ ← * <~ StoreCredits.create(
             Factories.storeCredit
               .copy(state = StoreCredit.Active, customerId = customer.id, originId = manual.id))
    } yield {}).gimme
    val refNum = cart.refNum
  }

  trait CreditCardFixture
      extends ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with CustomerAddress_Raw {
    val creditCard = CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).gimme
    val refNum     = cart.refNum
  }

  trait LineItemAndFundsFixture extends Reason_Baked with Customer_Seed {
    val (refNum, sku, creditCard, giftCard) = (for {
      address    ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      cc         ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
      productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      cart       ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
      product    ← * <~ Mvp.insertProduct(productCtx.id, Factories.products.head)
      sku        ← * <~ Skus.mustFindById404(product.skuId)
      manual ← * <~ StoreCreditManuals.create(
                  StoreCreditManual(adminId = storeAdmin.id, reasonId = reason.id))
      _ ← * <~ StoreCredits.create(
             Factories.storeCredit
               .copy(state = StoreCredit.Active, customerId = customer.id, originId = manual.id))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield (cart.refNum, sku, cc, giftCard)).gimme
  }

  trait ExpectedWarningsForPayment {
    def refNum: String
    def expectedWarnings = Seq(EmptyCart(refNum), NoShipAddress(refNum), NoShipMethod(refNum))
  }
}
