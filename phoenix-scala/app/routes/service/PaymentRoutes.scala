package routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.account.User
import payloads.CapturePayloads
import services.Authenticator.AuthData
import services.Capture
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.http.JsonSupport._

object PaymentRoutes {

  //TODO: Instead of store auth.model, add service accounts and require service JWT tokens.
  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route = {

    activityContext(auth) { implicit ac ⇒
      pathPrefix("capture") {
        (post & pathEnd & entity(as[CapturePayloads.Capture])) { payload ⇒
          mutateOrFailures {
            Capture.capture(payload)
          }
        }
      }
    }
  }
}
