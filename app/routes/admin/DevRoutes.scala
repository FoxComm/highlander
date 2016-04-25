package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads._
import services.orders.TimeMachine
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.aliases._

object DevRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("order-time-machine") {
        (post & pathEnd & entity(as[OrderTimeMachine])) { payload ⇒
          goodOrFailures {
            TimeMachine.changePlacedAt(payload.referenceNumber, payload.placedAt)
          }
        }
      }
    }
  }
}
