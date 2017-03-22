package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import testutils._
import cats.implicits._
import payloads.LineItemPayloads.UpdateLineItemsPayload

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  // move into object below @anna
  val rootPrefix: String = "v1/my"

  // move into object below @anna
  case class storefrontProductsApi(reference: String) {
    val productPath = s"$rootPrefix/products/$reference/baked"

    def get()(implicit ca: TestCustomerAuth): HttpResponse =
      GET(productPath, ca.jwtCookie.some)
  }

  object storefrontApi {

    object cart {
      val cartPath = s"$rootPrefix/cart"

      def get()(implicit ca: TestCustomerAuth): HttpResponse =
        GET(cartPath, ca.jwtCookie.some)

      object lineItems {
        val lineItemsPath = s"$cartPath/line-items"

        def add(payload: Seq[UpdateLineItemsPayload])(
            implicit ca: TestCustomerAuth): HttpResponse =
          POST(lineItemsPath, payload, ca.jwtCookie.some)
      }
    }
  }
}
