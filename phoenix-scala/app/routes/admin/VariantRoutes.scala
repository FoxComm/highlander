package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.account.User
import payloads.VariantPayloads._
import services.Authenticator.AuthData
import services.variant.VariantManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.JsonSupport._
import utils.json.codecs._

object VariantRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth) { implicit ac ⇒
      pathPrefix("variants") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[VariantPayload])) { payload ⇒
            mutateOrFailures {
              VariantManager.createVariant(context, payload)
            }
          } ~
          pathPrefix(IntNumber) { variantId ⇒
            (get & pathEnd) {
              getOrFailures {
                VariantManager.getVariant(context, variantId)
              }
            } ~
            (patch & pathEnd & entity(as[VariantPayload])) { payload ⇒
              mutateOrFailures {
                VariantManager.updateVariant(context, variantId, payload)
              }
            } ~
            pathPrefix("values") {
              (post & pathEnd & entity(as[VariantValuePayload])) { payload ⇒
                mutateOrFailures {
                  VariantManager.createVariantValue(context, variantId, payload)
                }
              }
            }
          }
        }
      }
    }
  }
}
