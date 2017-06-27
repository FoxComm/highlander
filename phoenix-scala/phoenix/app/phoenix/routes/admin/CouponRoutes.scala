package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.CouponPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.coupon.CouponManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object CouponRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("coupons") {

        pathPrefix("codes") {
          // TODO: get rid of this route → code is already provided in CouponResponse.Root @michalrus
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
