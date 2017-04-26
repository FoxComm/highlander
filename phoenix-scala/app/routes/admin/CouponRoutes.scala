package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.account.User
import payloads.CouponPayloads._
import services.Authenticator.AuthData
import services.coupon.CouponManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.JsonSupport._
import utils.json.codecs._

object CouponRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth) { implicit ac ⇒
      pathPrefix("coupons") {

        pathPrefix("codes") {
          pathPrefix("generate") {
            pathPrefix(IntNumber / Segment) { (id, code) ⇒
              (post & pathEnd) {
                mutateOrFailures {
                  CouponManager.generateCode(id, code, auth.model)
                }
              }
            } ~
            pathPrefix(IntNumber) { id ⇒
              (post & pathEnd & entity(as[GenerateCouponCodes])) { payload ⇒
                mutateOrFailures {
                  CouponManager.generateCodes(id, payload, auth.model)
                }
              }
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & pathEnd) {
              mutateOrFailures {
                CouponManager.getCodes(id)
              }
            }
          }
        } ~
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreateCoupon])) { payload ⇒
            mutateOrFailures {
              CouponManager.create(payload, context, Some(auth.model))
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & pathEnd) {
              getOrFailures {
                CouponManager.getIlluminated(id, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdateCoupon])) { payload ⇒
              mutateOrFailures {
                CouponManager.update(id, payload, context, auth.model)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                CouponManager.archiveByContextAndId(context, id)
              }
            }
          } ~
          pathPrefix(Segment) { code ⇒
            (get & pathEnd) {
              getOrFailures {
                CouponManager.getIlluminatedByCode(code, context)
              }
            }
          }
        }
      }
    }
  }
}
