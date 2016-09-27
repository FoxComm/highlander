import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import Extensions._
import cats.implicits._
import failures.CartFailures._
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
import utils.db._
import utils.seeds.CouponSeeds
import utils.seeds.Seeds.Factories

class CartValidatorIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AppendedClues
    with AutomaticAuth
    with BakedFixtures {

  "Cart validator must be applied to" - {

    "/v1/orders/:refNum/payment-methods/gift-cards" in new GiftCardFixture {
      val payload = GiftCardPayment(giftCard.code)
      checkResponse(POST(s"v1/orders/$refNum/payment-methods/gift-cards", payload),
                    expectedWarnings)
      checkResponse(DELETE(s"v1/orders/$refNum/payment-methods/gift-cards/${giftCard.code}"),
                    expectedWarnings)
    }

    "/v1/orders/:refNum/payment-methods/store-credit" in new StoreCreditFixture {
      val payload = StoreCreditPayment(500)
      checkResponse(POST(s"v1/orders/$refNum/payment-methods/store-credit", payload),
                    expectedWarnings)
      checkResponse(DELETE(s"v1/orders/$refNum/payment-methods/store-credit"), expectedWarnings)
    }

    "/v1/orders/:refNum/payment-methods/credit-cards" in new CreditCardFixture {
      val payload = CreditCardPayment(creditCard.id)
      checkResponse(POST(s"v1/orders/$refNum/payment-methods/credit-cards", payload),
                    expectedWarnings)
      checkResponse(DELETE(s"v1/orders/$refNum/payment-methods/credit-cards"), expectedWarnings)
    }

    "/v1/orders/:refNum/shipping-address" in new ShippingAddressFixture {
      val createPayload = CreateAddressPayload("a", 1, "b", None, "c", "11111")
      checkResponse(POST(s"v1/orders/$refNum/shipping-address", createPayload), expectedWarnings)
      val updatePayload = UpdateAddressPayload(name = "z".some)
      checkResponse(PATCH(s"v1/orders/$refNum/shipping-address", updatePayload), expectedWarnings)
      checkResponse(DELETE(s"v1/orders/$refNum/shipping-address"),
                    expectedWarnings :+ NoShipAddress(refNum))

      val address = Addresses.create(Factories.address.copy(accountId = customer.accountId)).gimme
      checkResponse(PATCH(s"v1/orders/$refNum/shipping-address/${address.id}"), expectedWarnings)
    }

    "/v1/orders/:refNum/shipping-method" in new ShippingMethodFixture {
      val payload = UpdateShippingMethod(shipMethod.id)
      checkResponse(PATCH(s"v1/orders/$refNum/shipping-method", payload),
                    Seq(EmptyCart(refNum), InsufficientFunds(refNum)))
      checkResponse(DELETE(s"v1/orders/$refNum/shipping-method"),
                    Seq(EmptyCart(refNum), NoShipMethod(refNum)))
    }

    "/v1/orders/:refNum/line-items" in new LineItemFixture {
      val payload = Seq(UpdateLineItemsPayload(sku.code, 1))
      checkResponse(POST(s"v1/orders/$refNum/line-items", payload),
                    Seq(InsufficientFunds(refNum), NoShipAddress(refNum), NoShipMethod(refNum)))
    }

    "/v1/orders/:refNum/coupon" in new CouponFixture {
      checkResponse(POST(s"v1/orders/$refNum/coupon/$couponCode"), expectedWarnings)
      checkResponse(DELETE(s"v1/orders/$refNum/coupon"), expectedWarnings)
    }

    "must validate funds with line items:" - {
      "must return warning when credit card is removed" in new LineItemAndFundsFixture {
        val lineItemPayload = Seq(UpdateLineItemsPayload(sku.code, 1))
        val response1       = POST(s"v1/orders/$refNum/line-items", lineItemPayload)
        response1.status must === (StatusCodes.OK)

        val ccPayload = CreditCardPayment(creditCard.id)
        val response2 = POST(s"v1/orders/$refNum/payment-methods/credit-cards", ccPayload)
        response2.status must === (StatusCodes.OK)

        checkResponse(DELETE(s"v1/orders/$refNum/payment-methods/credit-cards"),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }

      "must return warning when store credits are removed" in new LineItemAndFundsFixture {
        val lineItemPayload = Seq(UpdateLineItemsPayload(sku.code, 1))
        val response1       = POST(s"v1/orders/$refNum/line-items", lineItemPayload)
        response1.status must === (StatusCodes.OK)

        val scPayload = StoreCreditPayment(500)
        val response2 = POST(s"v1/orders/$refNum/payment-methods/store-credit", scPayload)
        response2.status must === (StatusCodes.OK)

        checkResponse(DELETE(s"v1/orders/$refNum/payment-methods/store-credit"),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }

      "must return warning when gift card is removed" in new LineItemAndFundsFixture {
        val lineItemPayload = Seq(UpdateLineItemsPayload(sku.code, 1))
        val response1       = POST(s"v1/orders/$refNum/line-items", lineItemPayload)
        response1.status must === (StatusCodes.OK)

        val gcPayload = GiftCardPayment(giftCard.code)
        val response2 = POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload)
        response2.status must === (StatusCodes.OK)

        checkResponse(DELETE(s"v1/orders/$refNum/payment-methods/gift-cards/${giftCard.code}"),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }
    }

    def checkResponse(response: HttpResponse, expectedWarnings: Seq[failures.Failure])(
        implicit line: sourcecode.Line,
        file: sourcecode.File) = {
      lazy val errorsStr: String = response.errors match {
        case Nil  ⇒ "No errors in response."
        case errs ⇒ s"""Errors: ${errs.mkString(", ")}"""
      }

      {
        response.status must === (StatusCodes.OK)
        val warnings = response.as[TheResponse[CartResponse]].warnings
        warnings.value must not be empty
        warnings.value must contain theSameElementsAs expectedWarnings.map(_.description)
      } withClue s"""
      | $errorsStr
      | (Original source: ${file.value.split("/").last}:${line.value})
      """.stripMargin
    }
  }

  trait CouponFixture
      extends CouponSeeds
      with TestActivityContext.AdminAC
      with ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with StoreAdmin_Seed {
    val (refNum, couponCode) = (for {
      search     ← * <~ Factories.createSharedSearches(storeAdmin.accountId)
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
                   Factories.address.copy(accountId = customer.accountId, regionId = 4129))
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
                  GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
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
                  StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      _ ← * <~ StoreCredits.create(
             Factories.storeCredit.copy(state = StoreCredit.Active,
                                        accountId = customer.accountId,
                                        originId = manual.id))
    } yield {}).gimme
    val refNum = cart.refNum
  }

  trait CreditCardFixture
      extends ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with CustomerAddress_Raw {
    val creditCard =
      CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId)).gimme
    val refNum = cart.refNum
  }

  trait LineItemAndFundsFixture extends Reason_Baked with Customer_Seed {
    val (refNum, sku, creditCard, giftCard) = (for {
      address    ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId))
      cc         ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId))
      productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      cart       ← * <~ Carts.create(Factories.cart.copy(accountId = customer.accountId))
      product    ← * <~ Mvp.insertProduct(productCtx.id, Factories.products.head)
      sku        ← * <~ Skus.mustFindById404(product.skuId)
      manual ← * <~ StoreCreditManuals.create(
                  StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      _ ← * <~ StoreCredits.create(
             Factories.storeCredit.copy(state = StoreCredit.Active,
                                        accountId = customer.accountId,
                                        originId = manual.id))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield (cart.refNum, sku, cc, giftCard)).gimme
  }

  trait ExpectedWarningsForPayment {
    def refNum: String
    def expectedWarnings = Seq(EmptyCart(refNum), NoShipAddress(refNum), NoShipMethod(refNum))
  }
}
