import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.AuthenticationResult
import akka.http.scaladsl.server.directives.SecurityDirectives.challengeFor

import models.StoreAdmin
import models.customer.Customer
import util.IntegrationTestBase

import services.Authenticator.AsyncAuthenticator

class RoutesAdminOnlyIntegrationTest extends IntegrationTestBase
  with HttpSupport {

  val authedStoreAdmin = StoreAdmin.build(id = 1, email = "donkey@donkey.com", password = Some("donkeyPass"),
    name = "Mister Donkey")

  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = AuthAs(authedStoreAdmin)
  override def overrideCustomerAuth: AsyncAuthenticator[Customer] = AuthFailWith[Customer](challengeFor("test"))

  "Requests with StoreAdmin only session (w/o customer)" - {
    "GET /v1/404alkjflskfdjg"  in {
      GET("v1/404alkjflskfdjg").status must === (StatusCodes.NotFound)
    }
  }
}


class RoutesCustomerOnlyIntegrationTest extends IntegrationTestBase
  with HttpSupport {

  val authedCustomer = Customer.build(id = 1, email = "donkey@donkey.com", password = Some("donkeyPass"),
    name = Some("Mister Donkey"))

  val uriPrefix = "v1/my"

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] = AuthAs(authedCustomer)
  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = AuthFailWith[StoreAdmin](challengeFor("test"))

  "Requests with Customer only session (w/o StoreAdmin)" - {
    s"GET ${uriPrefix}/404hello" in {
      GET(s"${uriPrefix}/404hello").status must === (StatusCodes.NotFound)
    }

    s"GET ${uriPrefix}/addresses" in {
      GET(s"${uriPrefix}/addresses").status must === (StatusCodes.OK)
    }
  }

}