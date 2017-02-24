import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import models.account._
import services.Authenticator.UserAuthenticator
import testutils._
import utils.MockedApis
import utils.seeds.Factories

class RoutesAdminOnlyIntegrationTest extends IntegrationTestBase with HttpSupport with MockedApis {

  val authedStoreAdmin =
    User(id = 1, accountId = 1, email = "admin@admin.com".some, name = "Mister Donkey".some)

  val authedCustomer =
    User(id = 2, accountId = 2, email = "donkey@donkey.com".some, name = "Mister Donkey".some)

  override def overrideUserAuth: UserAuthenticator =
    AuthAs(authedStoreAdmin, authedCustomer)

  "Requests with StoreAdmin only session (w/o customer)" - {
    "GET /v1/404alkjflskfdjg" in {
      GET("v1/404alkjflskfdjg").mustHaveStatus(StatusCodes.NotFound)
    }
  }
}

class RoutesCustomerOnlyIntegrationTest
    extends IntegrationTestBase
    with TestActivityContext.AdminAC
    with HttpSupport
    with MockedApis {

  val authedStoreAdmin =
    User(id = 1, accountId = 1, email = "admin@admin.com".some, name = "Mister Donkey".some)

  val authedCustomer =
    User(id = 1, accountId = 1, email = "donkey@donkey.com".some, name = "Mister Donkey".some)

  override def overrideUserAuth: UserAuthenticator =
    AuthAs(authedStoreAdmin, authedCustomer)

  "Requests with Customer only session (w/o StoreAdmin)" - {
    "GET v1/my/404hello" in {
      GET(s"v1/my/404hello").mustHaveStatus(StatusCodes.NotFound)
    }

    "GET v1/my/cart" in {
      Factories
        .createCustomer(user = authedCustomer,
                        isGuest = false,
                        scopeId = 2,
                        password = "password".some)
        .gimme
      GET(s"v1/my/cart").mustBeOk()
    }
  }
}
