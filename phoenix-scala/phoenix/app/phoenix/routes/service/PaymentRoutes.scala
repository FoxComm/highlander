package phoenix.routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.CapturePayloads
import phoenix.services.Authenticator.AuthData
import phoenix.services.Capture
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object PaymentRoutes {

  //TODO: Instead of store auth.model, add service accounts and require service JWT tokens.
  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
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
