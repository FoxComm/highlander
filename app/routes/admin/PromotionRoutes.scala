package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import payloads.{CreatePromotion, UpdatePromotion}
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import services.promotion.PromotionManager
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._

object PromotionRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, admin: StoreAdmin, apis: Apis) = {

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
