import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.directives.SecurityDirectives.challengeFor

import services.Authenticator.UserAuthenticator
import testutils._
import utils.MockedApis

class JsonRejectionHandlerIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with MockedApis {

  override def overrideUserAuth: UserAuthenticator =
    AuthFailWith(challengeFor("what ya doing!"))

  "JsonRejectionHandler should" - {
    "return a valid JSON rejection on 401 Unauthorized" in {
      val response = GET("v1/customers")

      response.mustHaveStatus(StatusCodes.Unauthorized)
      response.entity.contentType must === (ContentTypes.`application/json`)
      response.error must === ("The supplied authentication is invalid")
    }

    "return a valid JSON rejection on 404 NotFound" in {
      val response = GET("sdklgsdkvbnlsdkgmn")

      response.mustHaveStatus(StatusCodes.NotFound)
      response.entity.contentType must === (ContentTypes.`application/json`)
      response.error must === ("The requested resource could not be found.")
    }
  }
}
