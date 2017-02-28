package testutils.apis

import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, HttpResponse}
import payloads.CustomerPayloads.UpdateCustomerPayload
import testutils._
import cats.implicits._
import scala.collection.immutable

trait PhoenixMyApi extends HttpSupport { self: FoxSuite ⇒

  object myApi {
    val rootPrefix: String = "v1/my"

    object prepare {
      def myCart(): HttpRequest =
        buildRequest(HttpMethods.GET, s"$rootPrefix/cart")

      def myAddresses(): HttpRequest =
        buildRequest(HttpMethods.GET, s"$rootPrefix/addresses")

      def myAccount(): HttpRequest =
        buildRequest(HttpMethods.GET, s"$rootPrefix/account")

      def patchAccount(payload: UpdateCustomerPayload): HttpRequest =
        buildRequest(HttpMethods.PATCH, s"$rootPrefix/account", payload.some)
    }
  }

}
