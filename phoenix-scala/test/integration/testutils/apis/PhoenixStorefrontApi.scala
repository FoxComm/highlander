package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import testutils._

trait PhoenixStorefrontApi extends HttpSupport { self: FoxSuite ⇒

  val rootPrefix: String = "v1/my"

  case class storefrontProductsApi(reference: String) {
    val productPath = s"$rootPrefix/products/$reference/baked"

    def get: HttpResponse = GET(productPath)
  }
}
