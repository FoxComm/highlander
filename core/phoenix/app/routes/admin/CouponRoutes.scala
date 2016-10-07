package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.CouponPayloads._
import services.coupon.CouponManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CouponRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]) = {

    activityContext(auth.model) { implicit ac ⇒
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
        pathPrefix("forms" / IntNumber) { id ⇒
          (get & pathEnd) {
            mutateOrFailures {
              CouponManager.getForm(id)
            }
          }
        } ~
        pathPrefix("shadows" / Segment / IntNumber) { (context, id) ⇒
          (get & pathEnd) {
            mutateOrFailures {
              CouponManager.getShadow(id, context)
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
            (get & path("baked")) {
              getOrFailures {
                CouponManager.getIlluminated(id, context)
              }
            } ~
            (get & pathEnd) {
              getOrFailures {
                CouponManager.get(id, context)
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
