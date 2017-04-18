import cats.implicits._
import faker.Lorem
import models.location.Region
import models.shipping._
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CartPayloads.CreateCart
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LineItemPayloads._
import payloads.PaymentPayloads.CreateApplePayPayment
import payloads.UpdateShippingMethod
import responses.CustomerResponse
import responses.cord._
import services.StripeTest
import testutils._
import testutils.apis.PhoenixStorefrontApi
import testutils.fixtures.api._
import utils.MockedApis
import utils.seeds.{Factories, ShipmentSeeds}

class ApplePayIntegrationTest
    extends StripeTest
    with PhoenixStorefrontApi
    with ApiFixtures
    with MockedApis
    with AutomaticAuth {

  val apToken = "tok_1A9YBQJVm1XvTUrO3V8caBvF"

  "POST v1/my/payment-methods/apple-pay" - {
    "Apple pay checkout with funds authorized" in new ApplePayFixture {

      // test with cc token cause we can't create Apple Pay token, they act virtually the same tho
      val payment = CreateApplePayPayment(stripeToken = apToken)

      storefrontPaymentsApi.applePay.create(payment).mustBeOk()

      val skuInCart = cartsApi(refNum).checkout().as[OrderResponse].lineItems.skus.onlyElement
      skuInCart.sku must === (skuCode)
      skuInCart.quantity must === (2)

    }
  }

  "One click apple pay checkout should work" in new ApplePayFixture {
    val payment = CreateApplePayPayment(stripeToken = apToken)

    storefrontCartsApi.applePayCheckout(payment).as[OrderResponse].referenceNumber must === (
        cart.referenceNumber)
  }

  trait ApplePayFixture extends ProductSku_ApiFixture with ShipmentSeeds {
    val customer =
      customersApi.create(CreateCustomerPayload(email = "test@bar.com")).as[CustomerResponse.Root]

    val cart   = cartsApi.create(CreateCart(email = customer.email)).as[CartResponse]
    val refNum = cart.referenceNumber

    // we don't have shipping method API creation as of PR #910
    val shippingMethod: ShippingMethod = ShippingMethods
      .create(
          Factories.shippingMethods.head.copy(conditions = lowConditions.some,
                                              adminDisplayName =
                                                ShippingMethod.expressShippingNameForAdmin))
      .gimme

    val randomAddress = CreateAddressPayload(regionId = Region.californiaId,
                                             name = Lorem.letterify("???"),
                                             address1 = Lorem.letterify("???"),
                                             city = Lorem.letterify("???"),
                                             zip = Lorem.numerify("#####"))

    cartsApi(refNum).shippingAddress.create(randomAddress).mustBeOk()

    val lineItemsPayloads = List(UpdateLineItemsPayload(skuCode, 2))
    cartsApi(refNum).lineItems.add(lineItemsPayloads).mustBeOk()

    cartsApi(refNum).shippingMethod
      .update(UpdateShippingMethod(shippingMethod.id))
      .asTheResult[CartResponse]

  }
}
