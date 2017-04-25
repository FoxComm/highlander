package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import payloads.CartPayloads.CheckoutCart
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads.{CreateApplePayPayment, CreateCreditCardFromTokenPayload}
import testutils._

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  val rootPrefix: String = "v1/my"

  case class storefrontProductsApi(reference: String) {
    val productPath = s"$rootPrefix/products/$reference/baked"

    def get: HttpResponse = GET(productPath)
  }

  object storefrontCartsApi {
    val cartPath = s"$rootPrefix/cart"

    def get(): HttpResponse =
      GET(cartPath)

    def checkout(payload: CheckoutCart)(implicit ca: TestCustomerAuth): HttpResponse =
      POST(s"$cartPath/checkout", payload, ca.jwtCookie.some)

    def applePayCheckout(payload: CreateApplePayPayment)(
        implicit ca: TestCustomerAuth): HttpResponse =
      POST(s"$cartPath/apple-pay-checkout", payload, ca.jwtCookie.some)

    object lineItems {
      val lineItemsPath = s"$cartPath/line-items"

      def add(payload: Seq[UpdateLineItemsPayload])(implicit ca: TestCustomerAuth): HttpResponse =
        POST(lineItemsPath, payload, ca.jwtCookie.some)
    }
  }

  object storefrontAddressesApi {
    val addressPath = s"$rootPrefix/addresses"

    def unsetDefault(): HttpResponse =
      DELETE(s"$addressPath/default")
  }

  case class storefrontAddressesApi(id: Int) {
    val addressPath = s"${storefrontAddressesApi.addressPath}/$id"

    def setDefault(): HttpResponse =
      POST(s"$addressPath/default")
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

      def unsetDefault(): HttpResponse =
        DELETE(s"$ccPath/default")
    }

    case class creditCard(id: Int) {
      val ccPath = s"${creditCards.ccPath}/$id"

      def setDefault(): HttpResponse =
        POST(s"$ccPath/default")
    }
  }
}
