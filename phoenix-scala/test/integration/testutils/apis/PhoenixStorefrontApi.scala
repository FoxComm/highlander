package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import payloads.CartPayloads.CheckoutCart
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads.CreateCreditCardFromTokenPayload
import testutils._

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  val rootPrefix: String = "v1/my"

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

    object lineItems {
      val lineItemsPath = s"$cartPath/line-items"

      def add(payload: Seq[UpdateLineItemsPayload])(implicit ca: TestCustomerAuth): HttpResponse =
        POST(lineItemsPath, payload, ca.jwtCookie.some)
    }

    object shippingMethods{
      val shippingMethods = s"$cartPath/shipping-methods"

      def searchByRegion(countryCode: String)(implicit aa: TestCustomerAuth): HttpResponse =
        GET(s"$shippingMethods/$countryCode", aa.jwtCookie.some)

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

    object creditCards {
      val ccPath = s"$paymentPath/credit-cards"

      def get()(implicit ca: TestCustomerAuth): HttpResponse =
        GET(ccPath, ca.jwtCookie.some)

      def create(payload: CreateCreditCardFromTokenPayload)(
          implicit ca: TestCustomerAuth): HttpResponse =
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
