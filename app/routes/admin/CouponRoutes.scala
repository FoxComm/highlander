package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import payloads.{CreateCoupon, UpdateCoupon}
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import services.coupon.CouponManager
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._

object CouponRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, admin: StoreAdmin, apis: Apis) = {

      activityContext(admin) { implicit ac ⇒

        pathPrefix("coupons") {
          pathPrefix("forms" / IntNumber) { id ⇒
            (get & pathEnd) {
              goodOrFailures {
                CouponManager.getForm(id)
              }
            }
          } ~
          pathPrefix("shadows" / Segment / IntNumber) { (context, id)  ⇒
            (get & pathEnd) {
              goodOrFailures {
                CouponManager.getShadow(id, context)
              }
            }
          } ~
          pathPrefix(Segment) { (context)  ⇒
            (post & pathEnd & entity(as[CreateCoupon])) { payload ⇒
              goodOrFailures {
                CouponManager.create(payload, context)
              }
            } ~ 
            pathPrefix(IntNumber) { id ⇒ 
              (get & path("baked")) {
                goodOrFailures {
                  CouponManager.getIlluminated(id, context)
                }
              } ~
              (get & pathEnd) {
                goodOrFailures {
                  CouponManager.get(id, context)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateCoupon])) { payload ⇒
                goodOrFailures {
                  CouponManager.update(id, payload, context)
                }
              } 
            }
          }
        }
      }
  }
}
