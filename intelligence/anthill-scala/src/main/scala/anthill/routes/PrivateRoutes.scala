package anthill.routes

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives
import anthill.payloads.PurchaseEventPayload
import anthill.util.JsonSupport

object PrivateRoutes extends Directives with JsonSupport {
  val routes: Route = {
    pathPrefix("private") {
      pathPrefix("prod-prod") {
        (post & path("train") & pathEnd & entity(as[PurchaseEventPayload])) { payload =>
          complete(payload)
        }
      }
    }
  }
}
