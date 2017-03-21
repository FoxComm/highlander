package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import payloads.CartPayloads.CheckoutCart
import payloads.PaymentPayloads.ToggleDefaultCreditCard
import testutils._

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  val rootPrefix: String = "v1/my"

  case class storefrontProductsApi(reference: String) {
    val productPath = s"$rootPrefix/products/$reference/baked"

    def get: HttpResponse = GET(productPath)
  }

  object storefrontCartsApi {
    val cartPath = s"$rootPrefix/cart"

    def checkout(payload: CheckoutCart): HttpResponse =
      POST(s"$cartPath/checkout", payload)
  }

  object storefrontAddressesApi {
    val addressPath = s"$rootPrefix/addresses"
  }

  case class storefrontAddressesApi(id: Int) {
    val addressPath = s"${storefrontAddressesApi.addressPath}/$id"

    def setDefault(): HttpResponse =
      POST(s"$addressPath/default")
  }

  object storefrontPaymentsApi {
    val paymentPath = s"$rootPrefix/payment-methods"

    object creditCards {
      val ccPath = s"$paymentPath/credit-cards"
    }

    case class creditCard(id: Int) {
      val ccPath = s"${creditCards.ccPath}/$id"

      def toggleDefault(payload: ToggleDefaultCreditCard): HttpResponse =
        POST(s"$ccPath/default", payload)
    }
  }
}
