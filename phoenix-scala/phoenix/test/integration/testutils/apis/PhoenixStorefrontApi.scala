package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.CartPayloads.CheckoutCart
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.PaymentPayloads.{CreateApplePayPayment, CreateCreditCardFromTokenPayload}
import testutils._

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  val rootPrefix: String = "v1/my"

  object accountApi {
    val accountPath = s"$rootPrefix/account"

    def getAccount()(implicit ca: TestCustomerAuth): HttpResponse =
      GET(accountPath, ca.jwtCookie.some)
  }

  case class storefrontProductsApi(reference: String) {
    val productPath = s"$rootPrefix/products/$reference/baked"

    def get()(implicit ca: TestCustomerAuth): HttpResponse =
      GET(productPath, ca.jwtCookie.some)
  }

  object storefrontCartsApi {
    val cartPath = s"$rootPrefix/cart"

    def get()(implicit ca: TestCustomerAuth): HttpResponse =
      GET(cartPath, ca.jwtCookie.some)

    def checkout(payload: CheckoutCart)(implicit ca: TestCustomerAuth): HttpResponse =
      POST(s"$cartPath/checkout", payload, ca.jwtCookie.some)

    def applePayCheckout(payload: CreateApplePayPayment)(implicit ca: TestCustomerAuth): HttpResponse =
      POST(s"$cartPath/apple-pay-checkout", payload, ca.jwtCookie.some)

    object lineItems {
      val lineItemsPath = s"$cartPath/line-items"

      def add(payload: Seq[UpdateLineItemsPayload])(implicit ca: TestCustomerAuth): HttpResponse =
        POST(lineItemsPath, payload, ca.jwtCookie.some)
    }

    object shippingMethods {
      val shippingMethods = s"$cartPath/shipping-methods"

      def searchByRegion(countryCode: String)(implicit aa: TestCustomerAuth): HttpResponse =
        GET(s"$shippingMethods/$countryCode", aa.jwtCookie.some)
    }

    object shippingAddress {
      val shippingAddress = s"$cartPath/shipping-address"

      def create(payload: CreateAddressPayload)(implicit ca: TestCustomerAuth): HttpResponse =
        POST(shippingAddress, payload, ca.jwtCookie.some)

      def createOrUpdate(payload: CreateAddressPayload)(
          implicit ca: TestCustomerAuth): HttpResponse =
        PUT(shippingAddress, payload, ca.jwtCookie.some)
    }
  }

  object storefrontAddressesApi {
    val addressPath = s"$rootPrefix/addresses"

    def unsetDefault()(implicit ca: TestCustomerAuth): HttpResponse =
      DELETE(s"$addressPath/default", ca.jwtCookie.some)
  }

  case class storefrontAddressesApi(id: Int) {
    val addressPath = s"${storefrontAddressesApi.addressPath}/$id"

    def setDefault()(implicit ca: TestCustomerAuth): HttpResponse =
      POST(s"$addressPath/default", ca.jwtCookie.some)
  }

  object storefrontPaymentsApi {
    val paymentPath = s"$rootPrefix/payment-methods"

    object applePay {
      val path = s"$paymentPath/apple-pay"

      def create(p: CreateApplePayPayment)(implicit ca: TestCustomerAuth): HttpResponse =
        POST(path, p, ca.jwtCookie.some)
      def get()(implicit ca: TestCustomerAuth): HttpResponse    = GET(path, ca.jwtCookie.some)
      def delete()(implicit ca: TestCustomerAuth): HttpResponse = DELETE(path, ca.jwtCookie.some)
    }

    object creditCards {
      val ccPath = s"$paymentPath/credit-cards"

      def get()(implicit ca: TestCustomerAuth): HttpResponse =
        GET(ccPath, ca.jwtCookie.some)

      def create(payload: CreateCreditCardFromTokenPayload)(implicit ca: TestCustomerAuth): HttpResponse =
        POST(ccPath, payload, ca.jwtCookie.some)

      def unsetDefault()(implicit ca: TestCustomerAuth): HttpResponse =
        DELETE(s"$ccPath/default", ca.jwtCookie.some)
    }

    case class creditCard(id: Int) {
      val ccPath = s"${creditCards.ccPath}/$id"

      def setDefault()(implicit ca: TestCustomerAuth): HttpResponse =
        POST(s"$ccPath/default", ca.jwtCookie.some)
    }
  }
}
