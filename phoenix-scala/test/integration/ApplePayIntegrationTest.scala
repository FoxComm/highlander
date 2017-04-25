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
    with ApiFixtureHelpers
    with MockedApis
    with DefaultJwtAdminAuth {

  "POST v1/my/payment-methods/apple-pay" - {
    "Apple pay checkout with funds authorized" in new ProductSku_ApiFixture with ShipmentSeeds {
      val customer = customersApi
        .create(CreateCustomerPayload(email = "test@bar.com"))
        .as[CustomerResponse.Root]

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

      // test with cc token cause we can't create Apple Pay token, they act virtually the same tho
      val payment = CreateApplePayPayment(
          stripeToken = card.getId,
          stripeCustomerId = realStripeCustomerId,
          cartRef = refNum
      )

      val (customerResponse, customerLoginData) = api_newCustomerWithLogin()
      withCustomerAuth(customerLoginData, customerResponse.id) { implicit auth ⇒
        storefrontPaymentsApi.applePay.create(payment).mustBeOk()
      }

      val grandTotal = cartsApi(refNum).shippingMethod
        .update(UpdateShippingMethod(shippingMethod.id))
        .asTheResult[CartResponse]
        .totals
        .total

      val skuInCart = cartsApi(refNum).checkout().as[OrderResponse].lineItems.skus.onlyElement
      skuInCart.sku must === (skuCode)
      skuInCart.quantity must === (2)

    }
  }

}
