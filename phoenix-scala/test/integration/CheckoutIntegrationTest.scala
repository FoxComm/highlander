import java.time.Instant

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import util.Extensions._
import cats.implicits._
import failures.CustomerFailures._
import failures.NotFoundFailure404
import failures.ShippingMethodFailures.ShippingMethodNotFoundByName
import models.{Reason, Reasons}
import models.cord.Order.RemorseHold
import models.cord._
import models.customer.{Customer, Customers}
import models.inventory._
import models.location.{Address, Addresses}
import models.payment.giftcard._
import models.product.Mvp
import models.shipping._
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateCart
import payloads.PaymentPayloads.GiftCardPayment
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cord._
import slick.driver.PostgresDriver.api._
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "POST v1/orders/:refNum/checkout" - {

    "places order as admin" in new Fixture {
      val orderResponse = doCheckout(customer, sku, address, shipMethod, reason).as[OrderResponse]

      // Checkout:
      // Triggers cart → order transition
      Orders.findOneByRefNum(orderResponse.referenceNumber).gimme mustBe defined
      Carts.findOneByRefNum(orderResponse.referenceNumber).gimme must not be defined

      // Properly creates an order
      orderResponse.orderState must === (Order.RemorseHold)
      orderResponse.remorsePeriodEnd.value.isAfter(Instant.now) mustBe true

      // Authorizes payments
      GiftCardAdjustments.map(_.state).gimme must contain only GiftCardAdjustment.Auth
    }

    "fails if customer's credentials are empty" in new Fixture {
      val refNum = cartsApi.create(CreateCart(Some(customer.id))).as[CartResponse].referenceNumber

      Customers.update(customer, customer.copy(isGuest = true, email = None)).run().futureValue

      val checkout = cartsApi(refNum).checkout()
      checkout.error must === (CustomerMustHaveCredentials.description)
    }

    "fails if AFS is zero" in new Fixture {
      // FIXME #middlewarehouse
      pending

      doCheckout(customer, sku, address, shipMethod, reason)
        .as[OrderResponse]
        .orderState must === (RemorseHold)
    }

    "errors 404 if no cart found by reference number" in {
      val response = cartsApi("NOPE").checkout()
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
    }

    "fails if customer is blacklisted" in new BlacklistedFixture {
      val checkout = doCheckout(customer, sku, address, shipMethod, reason)
      checkout.status must === (StatusCodes.BadRequest)
      checkout.error must === (CustomerIsBlacklisted(customer.id).description)
    }

    def doCheckout(customer: Customer,
                   sku: Sku,
                   address: Address,
                   shipMethod: ShippingMethod,
                   reason: Reason): HttpResponse = {
      val refNum   = cartsApi.create(CreateCart(customer.id.some)).as[CartResponse].referenceNumber
      val _cartApi = cartsApi(refNum)

      _cartApi.lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 2))).mustBeOk()

      _cartApi.shippingAddress.updateFromAddress(address.id).mustBeOk()

      val grandTotal = _cartApi.shippingMethod
        .update(UpdateShippingMethod(shipMethod.id))
        .asTheResult[CartResponse]
        .totals
        .total

      val gcCode = giftCardsApi
        .create(GiftCardCreateByCsr(grandTotal, reason.id))
        .as[GiftCardResponse.Root]
        .code

      cartsApi(refNum).payments.giftCard.add(GiftCardPayment(gcCode, grandTotal.some)).mustBeOk()

      _cartApi.checkout()
    }
  }

  trait FullCartWithGcPayment
      extends Reason_Baked
      with EmptyCartWithShipAddress_Baked
      with FullCart_Raw
      with GiftCard_Raw
      with CartWithGiftCardPayment_Raw

  trait Fixture extends CustomerAddress_Baked with StoreAdmin_Seed {
    val (shipMethod, product, sku, reason) = (for {
      _ ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethodName = ShippingMethod.expressShippingNameForAdmin
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === shipMethodName)
                    .mustFindOneOr(ShippingMethodNotFoundByName(shipMethodName))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
    } yield (shipMethod, product, sku, reason)).gimme
  }

  trait BlacklistedFixture extends StoreAdmin_Seed {
    val (customer, address, shipMethod, product, sku, reason) = (for {
      customer ← * <~ Customers.create(
                    Factories.customer.copy(isBlacklisted = true,
                                            blacklistedBy = Some(storeAdmin.id)))
      address ← * <~ Addresses.create(Factories.usAddress1.copy(customerId = customer.id))
      _       ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === ShippingMethod.expressShippingNameForAdmin)
                    .mustFindOneOr(
                        ShippingMethodNotFoundByName(ShippingMethod.expressShippingNameForAdmin))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
    } yield (customer, address, shipMethod, product, sku, reason)).gimme
  }
}
