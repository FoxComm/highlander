package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.PromotionPayloads._
import services.promotion.PromotionManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object PromotionRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("promotions") {
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreatePromotion])) { payload ⇒
            mutateOrFailures {
              PromotionManager.create(payload, context)
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
                PromotionManager.update(id, payload, context)
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
