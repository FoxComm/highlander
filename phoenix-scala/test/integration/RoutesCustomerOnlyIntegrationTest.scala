import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import models.account.Users
import services.Authenticator.UserAuthenticator
import testutils.fixtures.BakedFixtures
import testutils._
import utils.MockedApis

class RoutesCustomerOnlyIntegrationTest
    extends IntegrationTestBase
    with TestActivityContext.AdminAC
    with BakedFixtures
    with HttpSupport
    with MockedApis {

  val authedCustomer = new Customer_Seed {}.customer

  override def overrideUserAuth: UserAuthenticator =
    AuthAs(None, authedCustomer.some)

  "Requests with Customer only session (w/o StoreAdmin)" - {

    "authorize (customer)" in new Customer_Seed {
      GET("v1/my/account").mustBeOk()
    }

    "handle not found correctly (customer)" - {

      "no prefix, wrong path (customer)" in {
        GET("foo").mustHaveStatus(StatusCodes.NotFound)
      }

      "/v1 prefix, wrong path (customer)" in {
        GET("v1/foo").mustHaveStatus(StatusCodes.NotFound)
      }

      "/v1/my prefix, wrong path (customer)" in {
        GET(s"v1/my/foo").mustHaveStatus(StatusCodes.NotFound)
      }
    }

    "are not allowed into anything besides /my (customer)" - {

      "/v1/admin prefix, ok path (customer)" in {
        GET("v1/admin/orders/123").mustHaveStatus(StatusCodes.Unauthorized)
      }

      "/v1/admin prefix, wrong path (customer)" in {
        GET("v1/admin/foo").mustHaveStatus(StatusCodes.Unauthorized)
      }
    }
  }
}
