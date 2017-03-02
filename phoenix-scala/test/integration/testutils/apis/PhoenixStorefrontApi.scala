package testutils.apis

import akka.http.scaladsl.model.HttpResponse

import testutils._

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  private val rootPrefix = "v1"

  case class storefrontProductsApi(reference: String) {
    val productPath = s"$rootPrefix/my/products/$reference/baked"

    def get(): HttpResponse = GET(productPath)
  }
}
