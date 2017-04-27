package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import models.account.User
import models.cord.Cord.cordRefNumRegex
import services.Authenticator.AuthData
import services.ShippingManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.json.codecs._

object ShippingMethodRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
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
}
