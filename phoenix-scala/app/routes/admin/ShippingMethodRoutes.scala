package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import models.account.User
import models.cord.Cord.cordRefNumRegex
import services.Authenticator.AuthData
import services.ShippingManager
import utils.aliases._
import utils.http.CustomDirectives._

object ShippingMethodRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("shipping-methods") {
        (get & pathEnd) {
          getOrFailures {
            ShippingManager.getActive
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
          } ~
          pathPrefix(IntNumber) { shippingMethodId ⇒
            (post & pathEnd) {
              mutateOrFailures {
                ShippingManager.setDefault(shippingMethodId = shippingMethodId)
              }
            }
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
