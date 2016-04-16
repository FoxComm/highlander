package routes

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import models.StoreAdmin
import payloads._
import services.NotificationManager
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._

object NotificationRoutes {

  def adminRoutes(implicit ec: EC, db: DB, mat: Materializer, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("notifications") {
        (get & path(IntNumber) & pathEnd) { adminId ⇒
          complete {
            NotificationManager.streamByAdminId(adminId)
          }
        } ~
        (get & pathEnd) {
          complete {
            NotificationManager.streamByAdminId(admin.id)
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

  def routes(implicit ec: EC, db: DB, mat: Materializer) = {

    activityContext() { implicit ac ⇒
      pathPrefix("public") {
        pathPrefix("notifications") {
          (post & pathEnd & entity(as[CreateNotification])) { payload ⇒
            goodOrFailures {
              NotificationManager.createNotification(payload)
            }
          }
        }
      }
    }
  }
}
