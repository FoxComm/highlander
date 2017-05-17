package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.utils.http.JsonSupport._
import phoenix.models.account.User
import phoenix.payloads.VariantPayloads._
import phoenix.services.variant.VariantManager
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._

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
