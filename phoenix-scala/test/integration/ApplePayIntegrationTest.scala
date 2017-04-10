import failures.ShippingMethodFailures.ShippingMethodNotFoundByName
import models.Reasons
import models.inventory._
import models.product.Mvp
import models.shipping._
import payloads.LineItemPayloads._
import payloads.PaymentPayloads.CreateApplePayPayment
import payloads.UpdateShippingMethod
import responses.cord._
import services.StripeTest
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixStorefrontApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtureHelpers
import utils.db._
import utils.seeds.Factories

class ApplePayIntegrationTest
    extends StripeTest
    with PhoenixStorefrontApi
    with ApiFixtureHelpers
    with AutomaticAuth
    with BakedFixtures {

  "POST v1/my/payment-methods/apple-pay" - {
    "Apple pay checkout with funds authorized" in new EmptyCartWithShipAddress_Baked with Fixture {
      val refNum = cart.referenceNumber

      private val lineItemsPayloads = List(UpdateLineItemsPayload(otherSku.code, 2))
      cartsApi(refNum).lineItems.add(lineItemsPayloads).mustBeOk()

      // test with cc token cause we can't create Apple Pay token, they act virtually the same tho
      private val payment = CreateApplePayPayment(
          token = card.getId,
          gatewayCustomerId = realStripeCustomerId,
          cartRef = refNum
      )

      storefrontPaymentsApi.applePay.create(payment).mustBeOk()

      val grandTotal = cartsApi(refNum).shippingMethod
        .update(UpdateShippingMethod(shipMethod.id))
        .asTheResult[CartResponse]
        .totals
        .total

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
