package routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import utils.http.JsonSupport._
import models.account.User
import services.Capture
import services.Authenticator.AuthData
import payloads.CapturePayloads
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object PaymentRoutes {

  //TODO: Instead of store auth.model, add service accounts and require service JWT tokens.
  def routes(implicit ec: EC, es: ES, db: DB, auth: AuthData[User], apis: Apis): Route = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("service") {
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
}
