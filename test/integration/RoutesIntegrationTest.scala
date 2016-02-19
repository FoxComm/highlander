import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.AuthenticationResult
import akka.http.scaladsl.server.directives.SecurityDirectives.challengeFor

import models.StoreAdmin
import models.customer.Customer
import util.IntegrationTestBase
import utils.Passwords.hashPassword

import services.Authenticator.AsyncAuthenticator

class RoutesAdminOnlyIntegrationTest extends IntegrationTestBase
  with HttpSupport {

  val authedStoreAdmin = StoreAdmin(id = 1, email = "donkey@donkey.com", password = hashPassword("donkeyPass"),
    name = "Mister Donkey")

  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = (UserCredentials) ⇒ {
    Future.successful(AuthenticationResult.success(authedStoreAdmin))
  }

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] = (UserCredentials) ⇒ {
    Future.successful(AuthenticationResult.failWithChallenge(challengeFor("test")))
  }

  "Requests with StoreAdmin only session (w/o customer)" - {
    "GET /v1/404alkjflskfdjg"  in {
      GET("v1/404alkjflskfdjg").status must === (StatusCodes.NotFound)
    }
  }
}


class RoutesCustomerOnlyIntegrationTest extends IntegrationTestBase
  with HttpSupport {

  val authedCustomer = Customer(id = 1, email = "donkey@donkey.com", password = Some(hashPassword("donkeyPass")),
    name = Some("Mister Donkey"))

  val uriPrefix = "v1/my"

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] = (UserCredentials) ⇒ {
    Future.successful(AuthenticationResult.success(authedCustomer))
  }

  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = (UserCredentials) ⇒ {
    Future.successful(AuthenticationResult.failWithChallenge(challengeFor("test")))
  }

  "Requests with Customer only session (w/o StoreAdmin)" - {
    s"GET ${uriPrefix}/404hello" in {
      GET(s"${uriPrefix}/404hello").status must === (StatusCodes.NotFound)
    }

    s"GET ${uriPrefix}/addresses" in {
      GET(s"${uriPrefix}/addresses").status must === (StatusCodes.OK)
    }
  }

}