package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.DiscountPayloads._
import services.discount.DiscountManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object DiscountRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("discounts") {
        pathPrefix("forms" / IntNumber) { id ⇒
          (get & pathEnd) {
            goodOrFailures {
              DiscountManager.getForm(id)
            }
          }
        } ~
        pathPrefix("shadows" / Segment / IntNumber) { (context, id) ⇒
          (get & pathEnd) {
            goodOrFailures {
              DiscountManager.getShadow(id, context)
            }
          }
        } ~
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreateDiscount])) { payload ⇒
            goodOrFailures {
              DiscountManager.create(payload, context)
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & path("baked")) {
              goodOrFailures {
                DiscountManager.getIlluminated(id, context)
              }
            } ~
            (get & pathEnd) {
              goodOrFailures {
                DiscountManager.get(id, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdateDiscount])) { payload ⇒
              goodOrFailures {
                DiscountManager.update(id, payload, context)
              }
            }
          }
        }
      }
    }
  }
}
