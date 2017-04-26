package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.account.User
import payloads.DiscountPayloads._
import services.Authenticator.AuthData
import services.discount.DiscountManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.JsonSupport._
import utils.json.codecs._

object DiscountRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth) { implicit ac ⇒
      pathPrefix("discounts") {
        pathPrefix("forms" / IntNumber) { id ⇒
          (get & pathEnd) {
            getOrFailures {
              DiscountManager.getForm(id)
            }
          }
        } ~
        pathPrefix("shadows" / Segment / IntNumber) { (context, id) ⇒
          (get & pathEnd) {
            getOrFailures {
              DiscountManager.getShadow(id, context)
            }
          }
        } ~
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreateDiscount])) { payload ⇒
            mutateOrFailures {
              DiscountManager.create(payload, context)
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & path("baked")) {
              getOrFailures {
                DiscountManager.getIlluminated(id, context)
              }
            } ~
            (get & pathEnd) {
              getOrFailures {
                DiscountManager.get(id, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdateDiscount])) { payload ⇒
              mutateOrFailures {
                DiscountManager.update(id, payload, context)
              }
            }
          }
        }
      }
    }
  }
}
