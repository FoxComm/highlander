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

object ProductOptionRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]) = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("product-options") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[ProductOptionPayload])) { payload ⇒
            mutateOrFailures {
              ProductOptionManager.create(context, payload)
            }
          } ~
          pathPrefix(IntNumber) { variantId ⇒
            (get & pathEnd) {
              getOrFailures {
                ProductOptionManager.get(context, variantId)
              }
            } ~
            (patch & pathEnd & entity(as[ProductOptionPayload])) { payload ⇒
              mutateOrFailures {
                ProductOptionManager.update(context, variantId, payload)
              }
            } ~
            pathPrefix("values") {
              (post & pathEnd & entity(as[ProductOptionValuePayload])) { payload ⇒
                mutateOrFailures {
                  ProductOptionManager.createProductOptionValue(context, variantId, payload)
                }
              }
            }
          }
        }
      }
    }
  }
}
