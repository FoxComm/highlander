package anthill.routes

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives
import anthill.responses.PingResponse
import anthill.util.JsonSupport

object PublicRoutes extends Directives with JsonSupport {
  // format: OFF
  val routes: Route =
    pathPrefix("public") {
      pathPrefix("ping") {
        (get & pathEnd) { complete(PingResponse("pong")) }
      } ~
      pathPrefix("prod-prod") {
        (get & path(IntNumber) & pathEnd) { productId =>
          complete(PingResponse(s"prod-prod with productId: ${productId}"))
        } ~
        pathPrefix("full") {
          (get & path(IntNumber) & pathEnd) { productId =>
            complete(PingResponse(s"prod-prod full with productId: ${productId}"))
          }
        }
      } ~
      pathPrefix("cust-prod") {
        (get & path(IntNumber) & pathEnd) { customerId =>
          complete(PingResponse(s"cust-prod with customerId: ${customerId}"))
        } ~
        pathPrefix("full") {
          (get & path(IntNumber) & pathEnd) { customerId =>
            complete(PingResponse(s"cust-prod full with customerId: ${customerId}"))
          }
        }
      }
    }
  // format: ON
}
