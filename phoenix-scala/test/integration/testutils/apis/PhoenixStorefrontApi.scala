package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import payloads.CustomerPayloads.UpdateCustomerPayload
import testutils._

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  val rootPrefix: String = "v1/my"

  def myCart() = GET(s"$rootPrefix/cart")

  def myAddresses() = GET(s"$rootPrefix/addresses")

  def myAccount() = GET(s"$rootPrefix/account")

  def patchAccount(payload: UpdateCustomerPayload) = PATCH(s"$rootPrefix/account", payload.some)
  case class storefrontProductsApi(reference: String) {
    val productPath = s"$rootPrefix/products/$reference/baked"

    def get: HttpResponse = GET(productPath)
  }
}
