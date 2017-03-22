import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import testutils._
import utils.MockedApis

class JsonRejectionHandlerIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with MockedApis {

  "JsonRejectionHandler should" - {
    "return a valid JSON rejection on 401 Unauthorized" in {
      val response = GET("v1/customers", jwtCookie = None)

      response.mustHaveStatus(StatusCodes.Unauthorized)
      response.entity.contentType must === (ContentTypes.`application/json`)
      response.error must === ("The supplied authentication is invalid")
    }

    "return a valid JSON rejection on 404 NotFound" in {
      val response = GET("sdklgsdkvbnlsdkgmn", jwtCookie = None)

      response.mustHaveStatus(StatusCodes.NotFound)
      response.entity.contentType must === (ContentTypes.`application/json`)
      response.error must === ("The requested resource could not be found.")
    }
  }
}
