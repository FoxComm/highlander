package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.ProductOptionPayloads._
import services.variant.ProductOptionManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object VariantRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]) = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("variants") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[ProductOptionPayload])) { payload ⇒
            mutateOrFailures {
              ProductOptionManager.createVariant(context, payload)
            }
          } ~
          pathPrefix(IntNumber) { variantId ⇒
            (get & pathEnd) {
              getOrFailures {
                ProductOptionManager.getVariant(context, variantId)
              }
            } ~
            (patch & pathEnd & entity(as[ProductOptionPayload])) { payload ⇒
              mutateOrFailures {
                ProductOptionManager.updateVariant(context, variantId, payload)
              }
            } ~
            pathPrefix("values") {
              (post & pathEnd & entity(as[ProductValuePayload])) { payload ⇒
                mutateOrFailures {
                  ProductOptionManager.createVariantValue(context, variantId, payload)
                }
              }
            }
          }
        }
      }
    }
  }
}
