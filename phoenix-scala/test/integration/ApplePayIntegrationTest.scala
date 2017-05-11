import cats.implicits._
import failures.GeneralFailure
import faker.Lorem
import models.location.Region
import models.shipping._
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CapturePayloads.{Capture, CaptureLineItem, ShippingCost}
import payloads.CartPayloads.CreateCart
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LineItemPayloads._
import payloads.PaymentPayloads.CreateApplePayPayment
import payloads.UpdateShippingMethod
import responses.{CaptureResponse, CustomerResponse}
import responses.cord._
import services.StripeTest
import testutils._
import testutils.apis.PhoenixStorefrontApi
import testutils.fixtures.api._
import utils.MockedApis
import utils.seeds.{Factories, ShipmentSeeds}

class ApplePayIntegrationTest
    extends IntegrationTestBase
    with PhoenixStorefrontApi
    with ApiFixtures
    with ApiFixtureHelpers
    with MockedApis
    with DefaultJwtAdminAuth {

  "POST v1/my/payment-methods/apple-pay" - {
    "Apple pay checkout with funds authorized" in new ApplePayFixture {

      val payment = CreateApplePayPayment(stripeToken = apToken)

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        storefrontPaymentsApi.applePay.create(payment).mustBeOk()
      }

      val skuInCart = cartsApi(refNum).checkout().as[OrderResponse].lineItems.skus.onlyElement
      skuInCart.sku must === (skuCode)
      skuInCart.quantity must === (1)

    }
  }

  "One click apple pay checkout should work" in new ApplePayFixture {
    val payment = CreateApplePayPayment(stripeToken = apToken)

    withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
      storefrontCartsApi.applePayCheckout(payment).as[OrderResponse].referenceNumber must === (
          cart.referenceNumber)
    }
  }

  "Fails with wrongly formatted token" in new ApplePayFixture {
    val payment = CreateApplePayPayment(stripeToken = "random token")

    val api = cartsApi(api_newCustomerCart(customer.id).referenceNumber)
    api.payments.applePay
      .add(payment)(defaultAdminAuth)
      .mustFailWith400(GeneralFailure("stripeTokenId should start with 'tok_'"))
  }

  "Capture Apple Pay" - {
    "Capture authorized payments" in new ApplePayFixture {
      val payment = CreateApplePayPayment(stripeToken = apToken)

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        val orderResponse = storefrontCartsApi.applePayCheckout(payment).as[OrderResponse]
        val skuInCart     = orderResponse.lineItems.skus

        val capturePayload =
          Capture(orderResponse.referenceNumber,
                  skuInCart.map(sku ⇒ CaptureLineItem(sku.referenceNumbers.head, sku.sku)),
                  ShippingCost(400, "USD"))

        val captureResponse = captureApi.capture(capturePayload).as[CaptureResponse]

        captureResponse.order must === (orderResponse.referenceNumber)
        captureResponse.captured must === (orderResponse.totals.total)
      }
    }
  }

  trait ApplePayFixture extends ProductSku_ApiFixture with ShipmentSeeds {
    val apToken           = "tok_1A9YBQJVm1XvTUrO3V8caBvF"
    val customerLoginData = TestLoginData(email = "test@bar.com", password = "pwd")
    val customer = customersApi
      .create(CreateCustomerPayload(email = customerLoginData.email,
                                    password = customerLoginData.password.some))
      .as[CustomerResponse.Root]

    val cart = cartsApi.create(CreateCart(customerId = customer.id.some)).as[CartResponse]

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

    val lineItemsPayloads = List(UpdateLineItemsPayload(skuCode, 1))
    cartsApi(refNum).lineItems.add(lineItemsPayloads).mustBeOk()

    cartsApi(refNum).shippingMethod
      .update(UpdateShippingMethod(shippingMethod.id))
      .asTheResult[CartResponse]

  }
}
