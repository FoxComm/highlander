import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import services.Authenticator.UserAuthenticator
import testutils._
import testutils.fixtures.BakedFixtures
import utils.MockedApis

class RoutesAdminOnlyIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with MockedApis
    with BakedFixtures {

  val authedAdmin = new StoreAdmin_Seed {}.storeAdmin

  override def overrideUserAuth: UserAuthenticator =
    AuthAs(authedAdmin.some, None)

  "Requests with StoreAdmin only session (w/o customer)" - {

    "are authorized (admin)" in {
      GET("v1/admin/groups").mustBeOk()
    }

    "handle not found correctly (admin)" - {

      "no prefix, wrong path (admin)" in {
        GET("foo").mustHaveStatus(StatusCodes.NotFound)
      }

      "/v1 prefix, wrong path (admin)" in {
        GET("v1/foo").mustHaveStatus(StatusCodes.NotFound)
      }

      "/v1/admin prefix, wrong path (admin)" in {
        GET("v1/admin/foo").mustHaveStatus(StatusCodes.NotFound)
      }
    }

    "can't access customer auth resources (admin)" - {

      "/v1/my prefix, ok path (admin)" in {
        GET("v1/my/account").mustHaveStatus(StatusCodes.Unauthorized)
      }

      "/v1/my prefix, wrong path (admin)" in {
        GET("v1/my/foo").mustHaveStatus(StatusCodes.Unauthorized)
      }
    }
  }
}
