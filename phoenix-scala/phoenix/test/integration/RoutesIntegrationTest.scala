import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import testutils._

class RoutesAdminOnlyIntegrationTest extends IntegrationTestBase with HttpSupport {

  "Requests with StoreAdmin only session (w/o customer)" - {
    "GET /v1/404alkjflskfdjg" in withRandomAdminAuth { auth ⇒
      GET("v1/404alkjflskfdjg", auth.jwtCookie.some).mustHaveStatus(StatusCodes.NotFound)
    }
  }
}

class RoutesCustomerOnlyIntegrationTest
    extends IntegrationTestBase
    with TestActivityContext.AdminAC
    with HttpSupport {

  "Requests with Customer only session (w/o StoreAdmin)" - {
    "GET v1/my/404hello" in withRandomCustomerAuth { auth ⇒
      GET(s"v1/my/404hello", auth.jwtCookie.some).mustHaveStatus(StatusCodes.NotFound)
    }

    "GET v1/my/cart" in withRandomCustomerAuth { auth ⇒
      GET(s"v1/my/cart", auth.jwtCookie.some).mustBeOk()
    }
  }
}
