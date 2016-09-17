package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.OrderPayloads.OrderTimeMachine
import services.orders.TimeMachine
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object DevRoutes {

  def routes(implicit ec: EC, db: DB, admin: User) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("order-time-machine") {
        (post & pathEnd & entity(as[OrderTimeMachine])) { payload ⇒
          mutateOrFailures {
            TimeMachine.changePlacedAt(payload.referenceNumber, payload.placedAt)
          }
        }
      }
    }
  }
}
