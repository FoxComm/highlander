package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import payloads.CustomerPayloads.UpdateCustomerPayload
import testutils._

trait PhoenixMyApi extends HttpSupport { self: FoxSuite ⇒

  object myApi {
    val rootPrefix: String = "v1/my"

    def myCart(): HttpResponse =
      GET(s"$rootPrefix/cart")

    def myAddresses(): HttpResponse =
      GET(s"$rootPrefix/addresses")

    def myAccount(): HttpResponse =
      GET(s"$rootPrefix/account")

    def patchAccount(payload: UpdateCustomerPayload): HttpResponse =
      PATCH(s"$rootPrefix/account", payload)
  }

}
