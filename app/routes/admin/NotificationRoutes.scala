package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import models.StoreAdmin
import payloads._
import services.NotificationManager
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.aliases._

object NotificationRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("notifications") {
        (get & pathEnd) {
          complete {
            NotificationManager.streamByAdminId(admin.id)
          }
        } ~
        (post & pathEnd & entity(as[CreateNotification])) { payload ⇒
          goodOrFailures {
            NotificationManager.createNotification(payload)
          }
        } ~
        (post & path("last-seen" / IntNumber) & pathEnd) { activityId ⇒
          goodOrFailures {
            NotificationManager.updateLastSeen(admin.id, activityId)
          }
        }
      }
    }
  }
}
