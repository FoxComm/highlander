package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.DiscountPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.discount.DiscountManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object DiscountRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
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
