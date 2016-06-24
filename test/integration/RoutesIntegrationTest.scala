import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.SecurityDirectives.challengeFor

import models.StoreAdmin
import models.customer.Customer
import services.Authenticator.AsyncAuthenticator
import util.IntegrationTestBase

class RoutesAdminOnlyIntegrationTest extends IntegrationTestBase with HttpSupport {

  val authedStoreAdmin = StoreAdmin.build(id = 1,
                                          email = "donkey@donkey.com",
                                          password = Some("donkeyPass"),
                                          name = "Mister Donkey")

  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] =
    AuthAs(authedStoreAdmin)
  override def overrideCustomerAuth: AsyncAuthenticator[Customer] =
    AuthFailWith[Customer](challengeFor("test"))

  "Requests with StoreAdmin only session (w/o customer)" - {
    "GET /v1/404alkjflskfdjg" in {
      GET("v1/404alkjflskfdjg").status must === (StatusCodes.NotFound)
    }
  }
}

class RoutesCustomerOnlyIntegrationTest extends IntegrationTestBase with HttpSupport {

  val authedCustomer = Customer.build(id = 1,
                                      email = "donkey@donkey.com",
                                      password = Some("donkeyPass"),
                                      name = Some("Mister Donkey"))

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] =
    AuthAs(authedCustomer)
  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] =
    AuthFailWith[StoreAdmin](challengeFor("test"))

  "Requests with Customer only session (w/o StoreAdmin)" - {
    "GET v1/my/404hello" in {
      GET(s"v1/my/404hello").status must === (StatusCodes.NotFound)
    }

    "GET v1/my/cart" in {
      GET(s"v1/my/cart").status must === (StatusCodes.OK)
    }
  }
}
