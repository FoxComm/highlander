import akka.http.scaladsl.model.{ContentTypes, StatusCodes}

import util.IntegrationTestBase

class JsonRejectionHandlerIntegrationTest extends IntegrationTestBase with HttpSupport {

  import Extensions._

  "JsonRejectionHandler should" - {
    "return a valid JSON rejection on 401 Unauthorized" in {
      val response = GET(s"v1/sdklgsdkvbnlsdkgmn")

      response.status must ===(StatusCodes.Unauthorized)
      response.entity.contentType must ===(ContentTypes.`application/json`)
      response.errors.head must ===("The resource requires authentication, which was not supplied with the request")
    }

    "return a valid JSON rejection on 404 NotFound" in {
      val response = GET(s"sdklgsdkvbnlsdkgmn")

      response.status must ===(StatusCodes.NotFound)
      response.entity.contentType must ===(ContentTypes.`application/json`)
      response.errors.head must ===("The requested resource could not be found.")
    }
  }

}
