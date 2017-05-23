package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.utils.http.JsonSupport._
import phoenix.models.account.User
import phoenix.payloads.PromotionPayloads._
import phoenix.services.promotion.PromotionManager
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._

object PromotionRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("promotions") {
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreatePromotion])) { payload ⇒
            mutateOrFailures {
              PromotionManager.create(payload, context, Some(auth.model))
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & pathEnd) {
              getOrFailures {
                PromotionManager.getIlluminated(id, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdatePromotion])) { payload ⇒
              mutateOrFailures {
                PromotionManager.update(id, payload, context, Some(auth.model))
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                PromotionManager.archiveByContextAndId(context, id)
              }
            }
          }
        }
      }
    }
  }
}
