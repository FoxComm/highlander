package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.CouponPayloads._
import services.coupon.CouponManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CouponRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("coupons") {

        pathPrefix("codes") {
          pathPrefix("generate") {
            pathPrefix(IntNumber / Segment) { (id, code) ⇒
              (post & pathEnd) {
                goodOrFailures {
                  CouponManager.generateCode(id, code, admin)
                }
              }
            } ~
            pathPrefix(IntNumber) { id ⇒
              (post & pathEnd & entity(as[GenerateCouponCodes])) { payload ⇒
                goodOrFailures {
                  CouponManager.generateCodes(id, payload, admin)
                }
              }
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & pathEnd) {
              goodOrFailures {
                CouponManager.getCodes(id)
              }
            }
          }
        } ~
        pathPrefix("forms" / IntNumber) { id ⇒
          (get & pathEnd) {
            goodOrFailures {
              CouponManager.getForm(id)
            }
          }
        } ~
        pathPrefix("shadows" / Segment / IntNumber) { (context, id) ⇒
          (get & pathEnd) {
            goodOrFailures {
              CouponManager.getShadow(id, context)
            }
          }
        } ~
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreateCoupon])) { payload ⇒
            goodOrFailures {
              CouponManager.create(payload, context, Some(admin))
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
                CouponManager.update(id, payload, context, admin)
              }
            }
          } ~
          pathPrefix(Segment) { code ⇒
            (get & pathEnd) {
              goodOrFailures {
                CouponManager.getIlluminatedByCode(code, context)
              }
            }
          }
        }
      }
    }
  }
}
