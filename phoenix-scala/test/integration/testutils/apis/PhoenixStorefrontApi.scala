package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import payloads.CartPayloads.CheckoutCart
import payloads.PaymentPayloads.CreateApplePayPayment
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

    def checkout(payload: CheckoutCart): HttpResponse =
      POST(s"$cartPath/checkout", payload)
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

      def post(p: CreateApplePayPayment): HttpResponse = POST(path, p)
      def get(): HttpResponse                          = GET(path)
      def delete(): HttpResponse                       = DELETE(path)
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
