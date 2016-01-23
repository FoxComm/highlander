import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._

import models.{Customer, StoreAdmin}
import util.IntegrationTestBase

class RoutesAdminOnlyIntegrationTest extends IntegrationTestBase
  with HttpSupport {

  val authedStoreAdmin = StoreAdmin(id = 1, email = "donkey@donkey.com", password = "donkeyPass",
    name = "Mister Donkey")

  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = (UserCredentials) ⇒ {
    Future.successful(Some(authedStoreAdmin))
  }

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] = (UserCredentials) ⇒ {
    Future.successful(None)
  }

  "GET /v1/404alkjflskfdjg" - {
    "Request to N.E. url should return 404 with storeAdmin creds. but w/o customerAuth"  in {
      GET("v1/404alkjflskfdjg").status === (StatusCodes.NotFound)
    }
  }
}
