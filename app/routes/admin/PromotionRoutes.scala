package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.PromotionPayloads._
import services.promotion.PromotionManager
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.aliases._

object PromotionRoutes {
  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("promotions") {
        pathPrefix("forms" / IntNumber) { id ⇒
          (get & pathEnd) {
            goodOrFailures {
              PromotionManager.getForm(id)
            }
          }
        } ~
        pathPrefix("shadows" / Segment / IntNumber) { (context, id)  ⇒
          (get & pathEnd) {
            goodOrFailures {
              PromotionManager.getShadow(id, context)
            }
          }
        } ~
        pathPrefix(Segment) { (context)  ⇒
          (post & pathEnd & entity(as[CreatePromotion])) { payload ⇒
            goodOrFailures {
              PromotionManager.create(payload, context)
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & path("baked")) {
              goodOrFailures {
                PromotionManager.getIlluminated(id, context)
              }
            } ~
            (get & pathEnd) {
              goodOrFailures {
                PromotionManager.get(id, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdatePromotion])) { payload ⇒
              goodOrFailures {
                PromotionManager.update(id, payload, context)
              }
            }
          }
        }
      }
    }
  }
}
