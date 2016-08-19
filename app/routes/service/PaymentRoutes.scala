package routes.service

import akka.http.scaladsl.server.Directives._

import cats.implicits._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import services.Capture
import payloads.CapturePayloads
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object PaymentRoutes {

  //TODO: Instead of store admin, add service accounts and require service JWT tokens.
  def routes(implicit ec: EC, es: ES, db: DB, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
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
