import java.time.Instant

import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import failures.AddressFailures.NoDefaultAddressForCustomer
import failures.CreditCardFailures.NoDefaultCreditCardForCustomer
import failures.NotFoundFailure404
import failures.ShippingMethodFailures.{NoDefaultShippingMethod, ShippingMethodNotFoundByName}
import failures.UserFailures._
import models.account._
import models.cord.Order.RemorseHold
import models.cord._
import models.cord.lineitems._
import models.customer._
import models.inventory._
import models.location.{Address, Addresses}
import models.payment.giftcard._
import models.payment.{InStorePaymentStates, PaymentMethod}
import models.product.Mvp
import models.shipping._
import models.{Reason, Reasons}
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CartPayloads.{CheckoutCart, CreateCart}
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads._
import payloads.PaymentPayloads.{CreateApplePayPayment, CreateCreditCardFromTokenPayload, GiftCardPayment}
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cord._
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixStorefrontApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtureHelpers
import utils.db._
import utils.seeds.Factories

class ApplePayIntegrationTest
    extends IntegrationTestBase
    with PhoenixStorefrontApi
    with ApiFixtureHelpers
    with AutomaticAuth
    with BakedFixtures {

  "POST v1/my/payment-methods/apple-pay" - {
    "Apple pay checkout with funds authorized" in new EmptyCartWithShipAddress_Baked with Fixture {

      val refNum = cart.referenceNumber

      private val lineItemsPayloads = List(UpdateLineItemsPayload(otherSku.code, 2))
      cartsApi(refNum).lineItems.add(lineItemsPayloads).mustBeOk()

      storefrontPaymentsApi.applePay
        .post(
            CreateApplePayPayment(
                token = "random",
                350000,
                cartRef = refNum
            ))
        .mustBeOk()

      val grandTotal = cartsApi(refNum).shippingMethod
        .update(UpdateShippingMethod(shipMethod.id))
        .asTheResult[CartResponse]
        .totals
        .total

      println(s"total price $grandTotal")

      cartsApi(refNum).checkout().as[OrderResponse].lineItems.skus.onlyElement must have(
          'sku (otherSku.code),
          'quantity (2)
      )

    }
  }

  trait Fixture extends StoreAdmin_Seed with CustomerAddress_Baked {
    val (shipMethod, product, sku, reason) = (for {
      _ ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethodName = ShippingMethod.expressShippingNameForAdmin
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === shipMethodName)
                    .mustFindOneOr(ShippingMethodNotFoundByName(shipMethodName))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
    } yield (shipMethod, product, sku, reason)).gimme

    val otherSku = (for {
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.tail.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
    } yield sku).gimme
  }

}
