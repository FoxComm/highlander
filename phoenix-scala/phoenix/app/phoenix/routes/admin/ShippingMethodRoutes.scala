package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import phoenix.models.account.User
import phoenix.models.cord.Cord.cordRefNumRegex
import phoenix.models.location.Country
import phoenix.services.Authenticator.AuthData
import phoenix.services.ShippingManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._

object ShippingMethodRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("shipping-methods") {
        (post & path(IntNumber / "default") & pathEnd) { shippingMethodId ⇒
          mutateOrFailures {
            ShippingManager.setDefault(shippingMethodId = shippingMethodId)
          }
        } ~
        pathPrefix("default") {
          (get & pathEnd) {
            getOrFailures {
              ShippingManager.getDefault
            }
          } ~
          (delete & pathEnd) {
            deleteOrFailures {
              ShippingManager.removeDefault()
            }
          }
        } ~
        (get & pathEnd) {
          getOrFailures {
            ShippingManager.getActive
          }
        } ~
        path(cordRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              ShippingManager.getShippingMethodsForCart(refNum)
            }
          }
        }
      }
    }
}
