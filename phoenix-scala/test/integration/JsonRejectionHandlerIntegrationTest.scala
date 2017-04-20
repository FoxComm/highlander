import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import cats.implicits._
import testutils._

class JsonRejectionHandlerIntegrationTest extends IntegrationTestBase with HttpSupport {

  "JsonRejectionHandler should" - {
    "return a valid JSON rejection on 401 Unauthorized" in withRandomCustomerAuth { auth ⇒
      val response = GET("v1/customers", jwtCookie = auth.jwtCookie.some)

      response.mustHaveStatus(StatusCodes.Unauthorized)
      response.entity.contentType must === (ContentTypes.`application/json`)
      response.error must === ("The supplied authentication is invalid")
    }

    "return a valid JSON rejection on 404 NotFound" in withRandomAdminAuth { auth ⇒
      val response = GET("sdklgsdkvbnlsdkgmn", jwtCookie = auth.jwtCookie.some)

      response.mustHaveStatus(StatusCodes.NotFound)
      response.entity.contentType must === (ContentTypes.`application/json`)
      response.error must === ("The requested resource could not be found.")
    }
  }
}
